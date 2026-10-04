package com.phongtro.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "vnpay_payments")
@Getter @Setter @NoArgsConstructor
public class VnpayPayment {
    @Id
    private String reference;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;
    @Column(nullable = false)
    private long amount;
    @Column(nullable = false)
    private String status = "PENDING";
    @Column(nullable = false)
    private Instant expiresAt;
    @Column(nullable = false)
    private Instant createdAt = Instant.now();
    private Instant completedAt;
    private String transactionNo;
    private String responseCode;
    private Instant lastQueriedAt;
    @Column(columnDefinition = "TEXT", nullable = false)
    private String paymentUrl;
}
