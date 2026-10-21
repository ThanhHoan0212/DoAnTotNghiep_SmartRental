ALTER TABLE chat_messages ADD COLUMN has_video BOOLEAN NOT NULL DEFAULT FALSE;
CREATE INDEX idx_chat_shared_media ON chat_messages(conversation_id, sent_at DESC, id DESC)
    WHERE has_image = TRUE OR has_video = TRUE;
