ALTER TABLE contracts ADD COLUMN agreed_end_date DATE;
ALTER TABLE contracts ADD COLUMN terminated_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE contracts ADD CONSTRAINT chk_agreed_end_date CHECK
    (agreed_end_date IS NULL OR (agreed_end_date >= start_date AND agreed_end_date < end_date));

CREATE TABLE contract_closure_requests (
    contract_id UUID NOT NULL REFERENCES contracts(id),
    request_order INTEGER NOT NULL,
    request_id UUID NOT NULL UNIQUE,
    kind VARCHAR(30) NOT NULL CHECK (kind IN ('CANCELLATION', 'EARLY_TERMINATION')),
    status VARCHAR(30) NOT NULL CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'COMPLETED', 'LAPSED')),
    requested_by UUID NOT NULL REFERENCES users(id),
    reason TEXT NOT NULL,
    requested_end_date DATE,
    refund_type VARCHAR(20) NOT NULL CHECK (refund_type IN ('FULL', 'PARTIAL', 'NONE')),
    refund_amount DOUBLE PRECISION NOT NULL CHECK (refund_amount >= 0 AND refund_amount < 'Infinity'::float8),
    settlement_note TEXT NOT NULL,
    requested_at TIMESTAMP WITH TIME ZONE NOT NULL,
    responded_by UUID REFERENCES users(id),
    responded_at TIMESTAMP WITH TIME ZONE,
    response_reason TEXT,
    completed_at TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (contract_id, request_order),
    CHECK (kind <> 'EARLY_TERMINATION' OR requested_end_date IS NOT NULL)
);
CREATE INDEX idx_contract_agreed_end_date ON contracts(agreed_end_date) WHERE status = 'ACTIVE';
CREATE UNIQUE INDEX uk_contract_open_closure ON contract_closure_requests(contract_id)
    WHERE status IN ('PENDING', 'ACCEPTED');
