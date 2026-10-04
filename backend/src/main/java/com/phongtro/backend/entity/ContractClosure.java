package com.phongtro.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** An immutable proposal after submission; only its response and completion fields change. */
@Embeddable
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ContractClosure {
    public enum Kind { CANCELLATION, EARLY_TERMINATION }
    public enum Status { PENDING, ACCEPTED, REJECTED, COMPLETED, LAPSED }
    public enum Refund { FULL, PARTIAL, NONE }

    @Column(nullable = false)
    private UUID requestId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private Kind kind;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private Status status;
    @Column(nullable = false)
    private UUID requestedBy;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;
    private LocalDate requestedEndDate;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private Refund refundType;
    @Column(nullable = false)
    private Double refundAmount;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String settlementNote;
    @Column(nullable = false)
    private Instant requestedAt;
    private UUID respondedBy;
    private Instant respondedAt;
    @Column(columnDefinition = "TEXT")
    private String responseReason;
    private Instant completedAt;
}
