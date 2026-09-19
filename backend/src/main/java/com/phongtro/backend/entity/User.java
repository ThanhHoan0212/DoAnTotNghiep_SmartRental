package com.phongtro.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_users_email", columnNames = "email"),
                @UniqueConstraint(name = "uq_users_phone", columnNames = "phone")
        },
        indexes = {
                @Index(name = "idx_users_email", columnList = "email"),
                @Index(name = "idx_users_phone", columnList = "phone"),
                @Index(name = "idx_users_role", columnList = "role"),
                @Index(name = "idx_users_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "email", nullable = false, length = 150)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    // eKYC (Electronic Know Your Customer) fields
    @Column(name = "is_identity_verified", nullable = false)
    @Builder.Default
    private boolean isIdentityVerified = false;

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

    @Column(name = "id_card_issue_date", length = 30)
    private String idCardIssueDate;

    @Column(name = "ekyc_confidence_score")
    private Double ekycConfidenceScore;

    @Column(name = "ekyc_verified_at")
    private java.time.Instant ekycVerifiedAt;

    @Column(name = "id_card_front_url", length = 500)
    private String idCardFrontUrl;

    @Column(name = "id_card_back_url", length = 500)
    private String idCardBackUrl;

    @Column(name = "selfie_url", length = 500)
    private String selfieUrl;
}
