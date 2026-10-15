-- Migration V15: VNPT SmartCA Digital Signatures
ALTER TABLE contracts
    ADD COLUMN tenant_signature TEXT,
    ADD COLUMN landlord_signature TEXT,
    ADD COLUMN tenant_sign_txn_id VARCHAR(255),
    ADD COLUMN landlord_sign_txn_id VARCHAR(255),
    ADD COLUMN tenant_cert_serial VARCHAR(255),
    ADD COLUMN landlord_cert_serial VARCHAR(255);
