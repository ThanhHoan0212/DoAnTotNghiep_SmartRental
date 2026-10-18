package com.phongtro.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Entity
@Table(name = "support_complaints")
@Getter @Setter
public class SupportComplaint extends BaseEntity {
    public enum Status { OPEN, IN_PROGRESS, RESOLVED, REJECTED }
    public enum Category { FRAUD, LISTING, ACCOUNT, PAYMENT, OTHER }
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id")
    private User reporter;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private Contract contract;
    @Column(nullable = false, length = 200)
    private String subject;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private Category category;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private Status status = Status.OPEN;
    @Column(columnDefinition = "TEXT")
    private String response;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "handled_by")
    private User handledBy;
    @Version
    private long version;
}
