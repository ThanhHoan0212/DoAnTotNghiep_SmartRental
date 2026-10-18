-- Retain existing room conversations and allow direct chats between any roles.
ALTER TABLE chat_conversations ALTER COLUMN room_id DROP NOT NULL;
CREATE UNIQUE INDEX uq_chat_direct_pair
    ON chat_conversations (LEAST(tenant_id, landlord_id), GREATEST(tenant_id, landlord_id))
    WHERE room_id IS NULL;
