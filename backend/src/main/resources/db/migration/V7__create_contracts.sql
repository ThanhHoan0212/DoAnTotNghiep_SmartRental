-- ==============================================================================
-- Flyway Migration V7: Quản lý Hợp đồng thuê phòng (contracts) & Cập nhật trạng thái phòng (RENTED)
-- ==============================================================================

-- 1. Bổ sung trạng thái RENTED (Đã có người thuê) vào bảng rooms
ALTER TABLE rooms DROP CONSTRAINT IF EXISTS chk_rooms_status;
ALTER TABLE rooms ADD CONSTRAINT chk_rooms_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'HIDDEN', 'RENTED'));

-- 2. Bảng quản lý hợp đồng thuê phòng (contracts)
CREATE TABLE IF NOT EXISTS contracts (
    id UUID PRIMARY KEY,
    contract_code VARCHAR(50) UNIQUE NOT NULL,
    tenant_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    landlord_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    room_id UUID NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    monthly_rent DOUBLE PRECISION NOT NULL,
    deposit_amount DOUBLE PRECISION NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    terms TEXT,
    cancellation_reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_contracts_status CHECK (status IN ('PENDING', 'ACTIVE', 'REJECTED', 'EXPIRED', 'TERMINATED', 'CANCELLED')),
    CONSTRAINT chk_contract_dates CHECK (end_date > start_date)
);

-- 3. Tạo các chỉ mục tối ưu hóa tốc độ truy vấn
CREATE INDEX IF NOT EXISTS idx_contracts_tenant_id ON contracts(tenant_id);
CREATE INDEX IF NOT EXISTS idx_contracts_landlord_id ON contracts(landlord_id);
CREATE INDEX IF NOT EXISTS idx_contracts_room_id ON contracts(room_id);
CREATE INDEX IF NOT EXISTS idx_contracts_status ON contracts(status);
CREATE INDEX IF NOT EXISTS idx_contracts_code ON contracts(contract_code);
