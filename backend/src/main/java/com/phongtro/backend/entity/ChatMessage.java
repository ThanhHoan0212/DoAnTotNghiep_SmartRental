package com.phongtro.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "chat_messages")
@Getter @Setter @NoArgsConstructor
public class ChatMessage {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "conversation_id")
    private ChatConversation conversation;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "sender_id")
    private User sender;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    @Column(name = "has_image", nullable = false)
    private boolean hasImage;
    @Column(name = "has_video", nullable = false)
    private boolean hasVideo;
    @Column(nullable = false)
    private boolean recalled;
    @Version
    private long revision;
    @ElementCollection
    @org.hibernate.annotations.BatchSize(size = 50)
    @CollectionTable(name = "chat_reactions", joinColumns = @JoinColumn(name = "message_id"))
    @MapKeyColumn(name = "user_id")
    @Column(name = "emoji", nullable = false, length = 16)
    private java.util.Map<UUID, String> reactions = new java.util.LinkedHashMap<>();
    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;
}
