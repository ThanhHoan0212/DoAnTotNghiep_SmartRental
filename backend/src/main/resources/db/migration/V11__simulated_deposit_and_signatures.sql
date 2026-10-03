ALTER TABLE contracts DROP CONSTRAINT chk_contracts_status;
ALTER TABLE contracts ADD CONSTRAINT chk_contracts_status CHECK
    (status IN ('PENDING', 'AWAITING_DEPOSIT', 'AWAITING_SIGNATURES', 'ACTIVE', 'REJECTED', 'EXPIRED', 'TERMINATED', 'CANCELLED'));
ALTER TABLE rooms DROP CONSTRAINT chk_rooms_status;
ALTER TABLE rooms ADD CONSTRAINT chk_rooms_status CHECK
    (status IN ('PENDING', 'APPROVED', 'REJECTED', 'HIDDEN', 'RESERVED', 'RENTED'));

ALTER TABLE contracts
    ADD COLUMN request_code VARCHAR(255),
    ADD COLUMN deposit_deadline TIMESTAMP WITH TIME ZONE,
    ADD COLUMN deposit_paid_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN payment_reference VARCHAR(255),
    ADD COLUMN formalized_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN tenant_signed_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN landlord_signed_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN activated_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN document_content TEXT;

-- Preserve legacy agreements without fabricating payments or signatures.
UPDATE contracts SET request_code = contract_code;
CREATE UNIQUE INDEX uk_room_single_reserved_or_active_contract ON contracts(room_id)
    WHERE status IN ('AWAITING_DEPOSIT', 'AWAITING_SIGNATURES', 'ACTIVE');
