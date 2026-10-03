CREATE TABLE vnpay_payments (
    reference VARCHAR(255) PRIMARY KEY,
    contract_id UUID NOT NULL REFERENCES contracts(id),
    amount BIGINT NOT NULL CHECK (amount > 0),
    status VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    transaction_no VARCHAR(255),
    response_code VARCHAR(255),
    payment_url TEXT NOT NULL
);
CREATE INDEX idx_vnpay_contract_status ON vnpay_payments(contract_id, status, created_at);
