package com.phongtro.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "chat_conversations", uniqueConstraints = @UniqueConstraint(columnNames = {"room_id", "tenant_id"}))
@Getter @Setter @NoArgsConstructor
public class ChatConversation extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "room_id")
    private Room room;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "tenant_id")
    private User tenant;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "landlord_id")
    private User landlord;
}
