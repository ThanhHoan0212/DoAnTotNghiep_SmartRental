package com.phongtro.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "contracts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Contract extends BaseEntity {

    @ElementCollection
    @CollectionTable(name = "contract_closure_requests", joinColumns = @JoinColumn(name = "contract_id"))
    @OrderColumn(name = "request_order")
    @Builder.Default
    private java.util.List<ContractClosure> closureRequests = new java.util.ArrayList<>();

    private LocalDate agreedEndDate;
    private java.time.Instant terminatedAt;

    private String requestCode;
    private java.time.Instant depositDeadline;
    private java.time.Instant depositPaidAt;
    private String paymentReference;
    private java.time.Instant formalizedAt;
    private java.time.Instant tenantSignedAt;
    private java.time.Instant landlordSignedAt;
    private java.time.Instant activatedAt;

    @Column(columnDefinition = "TEXT")
    private String documentContent;


    @Version
    @Column(nullable = false)
    private long version;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "contract_code", nullable = false, unique = true, length = 50)
    private String contractCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private User tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "landlord_id", nullable = false)
    private User landlord;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "monthly_rent", nullable = false)
    private Double monthlyRent;

    @Column(name = "deposit_amount", nullable = false)
    private Double depositAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ContractStatus status;

    @Column(name = "terms", columnDefinition = "TEXT")
    private String terms;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;
}
