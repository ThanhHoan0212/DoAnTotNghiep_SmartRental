ALTER TABLE contracts ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- A listing or account must not silently erase rental agreements.
ALTER TABLE contracts DROP CONSTRAINT contracts_room_id_fkey;
ALTER TABLE contracts ADD CONSTRAINT contracts_room_id_fkey
    FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE RESTRICT;
ALTER TABLE contracts DROP CONSTRAINT contracts_tenant_id_fkey;
ALTER TABLE contracts ADD CONSTRAINT contracts_tenant_id_fkey
    FOREIGN KEY (tenant_id) REFERENCES users(id) ON DELETE RESTRICT;
ALTER TABLE contracts DROP CONSTRAINT contracts_landlord_id_fkey;
ALTER TABLE contracts ADD CONSTRAINT contracts_landlord_id_fkey
    FOREIGN KEY (landlord_id) REFERENCES users(id) ON DELETE RESTRICT;
