package com.phongtro.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "ekyc_verifications",
        indexes = {
                @Index(name = "idx_ekyc_verifications_user", columnList = "user_id, created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EkycVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "id_card_number", length = 20)
    private String idCardNumber;

    @Column(name = "id_card_name", length = 150)
    private String idCardName;

    @Column(name = "id_card_dob", length = 20)
    private String idCardDob;

    @Column(name = "id_card_address", length = 255)
    private String idCardAddress;

    @Column(name = "id_card_hometown", length = 255)
    private String idCardHometown;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "status", nullable = false, length = 20)
    private String status; // SUCCESS, FAILED, PENDING

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "raw_ocr_response", columnDefinition = "TEXT")
    private String rawOcrResponse;

    @Column(name = "raw_face_response", columnDefinition = "TEXT")
    private String rawFaceResponse;

    @Column(name = "front_image_url", length = 500)
    private String frontImageUrl;

    @Column(name = "back_image_url", length = 500)
    private String backImageUrl;

    @Column(name = "selfie_image_url", length = 500)
    private String selfieImageUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
