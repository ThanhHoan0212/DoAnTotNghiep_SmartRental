ALTER TABLE support_complaints
    ADD COLUMN contract_id UUID REFERENCES contracts(id) ON DELETE SET NULL;
CREATE INDEX idx_support_complaints_contract_reporter
    ON support_complaints(contract_id, reporter_id, created_at DESC);
