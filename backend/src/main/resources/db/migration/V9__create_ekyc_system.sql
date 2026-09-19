-- V9: Tích hợp định danh điện tử eKYC qua FPT.AI (CCCD OCR & Face Matching > 85%)

-- 1. Bổ sung các cột định danh vào bảng users
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS is_identity_verified BOOLEAN DEFAULT FALSE NOT NULL,
    ADD COLUMN IF NOT EXISTS id_card_number VARCHAR(20),
    ADD COLUMN IF NOT EXISTS id_card_name VARCHAR(150),
    ADD COLUMN IF NOT EXISTS id_card_dob VARCHAR(20),
    ADD COLUMN IF NOT EXISTS id_card_address VARCHAR(255),
    ADD COLUMN IF NOT EXISTS id_card_hometown VARCHAR(255),
    ADD COLUMN IF NOT EXISTS id_card_issue_date VARCHAR(30),
    ADD COLUMN IF NOT EXISTS ekyc_confidence_score DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS ekyc_verified_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS id_card_front_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS id_card_back_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS selfie_url VARCHAR(500);

-- Tạo index cho số CCCD để kiểm tra trùng lặp
CREATE INDEX IF NOT EXISTS idx_users_id_card_number ON users(id_card_number);

-- 2. Tạo bảng lưu trữ lịch sử xác thực eKYC (Audit Log & Verification History)
CREATE TABLE IF NOT EXISTS ekyc_verifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    id_card_number VARCHAR(20),
    id_card_name VARCHAR(150),
    id_card_dob VARCHAR(20),
    id_card_address VARCHAR(255),
    id_card_hometown VARCHAR(255),
    confidence_score DOUBLE PRECISION,
    status VARCHAR(20) NOT NULL, -- PENDING, SUCCESS, FAILED
    failure_reason TEXT,
    raw_ocr_response TEXT,
    raw_face_response TEXT,
    front_image_url VARCHAR(500),
    back_image_url VARCHAR(500),
    selfie_image_url VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ekyc_verifications_user ON ekyc_verifications(user_id, created_at DESC);
