package com.phongtro.backend.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.UUID;

public final class ChatDtos {
    private ChatDtos() {}
    public record SendRequest(@NotBlank String content) {}
    public record Conversation(UUID id, UUID roomId, String roomTitle, UUID otherUserId,
                               String otherUserName, String otherUserEmail, String otherUserAvatarUrl, String otherUserRole,
                               boolean otherUserIdentityVerified, Instant otherUserCreatedAt, Instant updatedAt) {}
    public record Reaction(UUID userId, String emoji) {}
    public record ReactionRequest(@NotBlank String emoji) {}
    public record Message(UUID id, UUID senderId, String content, Instant sentAt, boolean hasImage, boolean hasVideo,
                          boolean recalled, long revision, java.util.List<Reaction> reactions) {}
    public record ImageData(String contentType, byte[] data) {}
    public record ImagePage(java.util.List<Message> items, boolean hasMore) {}
}
