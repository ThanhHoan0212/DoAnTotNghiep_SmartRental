-- ==============================================================================
-- Flyway Migration V1: Khoi tao bang users va refresh_tokens
-- ==============================================================================

-- Bat extension uuid-ossp hoac pgcrypto de ho tro gen_random_uuid() tren PostgreSQL
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 1. Tao bang users
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(150) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    avatar_url VARCHAR(500),
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT uq_users_phone UNIQUE (phone),
    CONSTRAINT chk_users_role CHECK (role IN ('TENANT', 'LANDLORD', 'ADMIN')),
    CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'BANNED'))
);

-- Index ho tro tim kiem va xac thuc nhanh
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_phone ON users(phone);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);
CREATE INDEX IF NOT EXISTS idx_users_status ON users(status);

-- 2. Tao bang refresh_tokens
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    token VARCHAR(255) NOT NULL,
    expiry_date TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_refresh_tokens_token UNIQUE (token),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON refresh_tokens(user_id);

-- 3. Seed tai khoan Admin khoi tao (Email: admin@phongtro.vn, Mat khau: Admin@123)
-- Password Hash duoc ma hoa bang BCrypt voi cost factor 10 cho chuoi 'Admin@123'
INSERT INTO users (id, email, password_hash, full_name, phone, role, status, created_at, updated_at)
VALUES (
    'a0000000-0000-0000-0000-000000000001',
    'admin@phongtro.vn',
    '$2a$10$wO3l79wYl75c9s2wY4Zc..9L1sZ8c6sA0F2vY9L2y3r6w0bY8Y5k2',
    'Hệ Thống Quản Trị Viên',
    '0900000000',
    'ADMIN',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (email) DO NOTHING;
