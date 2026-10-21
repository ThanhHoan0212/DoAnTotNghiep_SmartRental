CREATE TABLE chat_conversations (
    id UUID PRIMARY KEY,
    room_id UUID NOT NULL REFERENCES rooms(id),
    tenant_id UUID NOT NULL REFERENCES users(id),
    landlord_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_chat_room_tenant UNIQUE (room_id, tenant_id),
    CONSTRAINT ck_chat_participants CHECK (tenant_id <> landlord_id)
);
CREATE INDEX idx_chat_tenant ON chat_conversations(tenant_id, updated_at DESC);
CREATE INDEX idx_chat_landlord ON chat_conversations(landlord_id, updated_at DESC);
CREATE TABLE chat_messages (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL REFERENCES chat_conversations(id),
    sender_id UUID NOT NULL REFERENCES users(id),
    content VARCHAR(2000) NOT NULL CHECK (length(trim(content)) > 0),
    sent_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_chat_messages_history ON chat_messages(conversation_id, sent_at DESC, id DESC);
