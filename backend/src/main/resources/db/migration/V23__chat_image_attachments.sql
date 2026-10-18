ALTER TABLE chat_messages ADD COLUMN has_image BOOLEAN NOT NULL DEFAULT FALSE;
CREATE TABLE chat_images (
    message_id UUID PRIMARY KEY REFERENCES chat_messages(id) ON DELETE CASCADE,
    content_type VARCHAR(255) NOT NULL,
    data BYTEA NOT NULL
);
