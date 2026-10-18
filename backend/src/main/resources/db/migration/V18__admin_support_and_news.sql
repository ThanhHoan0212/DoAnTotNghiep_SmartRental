CREATE TABLE support_complaints (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reporter_id UUID NOT NULL REFERENCES users(id),
    room_id UUID REFERENCES rooms(id) ON DELETE SET NULL,
    subject VARCHAR(200) NOT NULL,
    category VARCHAR(30) NOT NULL CHECK (category IN ('FRAUD','LISTING','ACCOUNT','PAYMENT','OTHER')),
    description TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN','IN_PROGRESS','RESOLVED','REJECTED')),
    response TEXT,
    handled_by UUID REFERENCES users(id),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_support_complaints_status_created ON support_complaints(status, created_at DESC);
CREATE INDEX idx_support_complaints_reporter ON support_complaints(reporter_id, created_at DESC);

CREATE TABLE rental_news (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(200) NOT NULL,
    summary VARCHAR(500) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','PUBLISHED')),
    author_id UUID NOT NULL REFERENCES users(id),
    published_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_rental_news_status_created ON rental_news(status, created_at DESC);
