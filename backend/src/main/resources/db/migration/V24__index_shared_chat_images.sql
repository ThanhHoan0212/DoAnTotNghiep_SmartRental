CREATE INDEX idx_chat_shared_images ON chat_messages(conversation_id, sent_at DESC, id DESC)
    WHERE has_image = TRUE;
