ALTER TABLE chat_messages ADD COLUMN recalled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE chat_messages ADD COLUMN revision BIGINT NOT NULL DEFAULT 0;
CREATE TABLE chat_reactions (
    message_id UUID NOT NULL REFERENCES chat_messages(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id),
    emoji VARCHAR(16) NOT NULL,
    PRIMARY KEY (message_id, user_id)
);
