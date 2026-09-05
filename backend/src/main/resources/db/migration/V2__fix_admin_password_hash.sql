-- ==============================================================================
-- Flyway Migration V2: Sua lai password_hash cho tai khoan Admin (Admin@123)
-- ==============================================================================

UPDATE users
SET password_hash = '$2a$10$efJsXiQ93Fx5JhldHrd4POdKShTbmr9yIdDzs2llPZXj1R2sz8c2u',
    status = 'ACTIVE',
    updated_at = CURRENT_TIMESTAMP
WHERE email = 'admin@phongtro.vn';
