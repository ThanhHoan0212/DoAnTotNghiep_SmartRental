package com.phongtro.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity @Table(name = "chat_images") @Getter @Setter @NoArgsConstructor
public class ChatImage {
    @Id @Column(name = "message_id")
    private UUID messageId;
    @Column(name = "content_type", nullable = false)
    private String contentType;
    @Column(nullable = false, columnDefinition = "bytea")
    private byte[] data;
}
