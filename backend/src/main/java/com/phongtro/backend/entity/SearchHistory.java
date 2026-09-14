package com.phongtro.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "search_histories",
        indexes = {
                @Index(name = "idx_search_histories_user_id", columnList = "user_id"),
                @Index(name = "idx_search_histories_searched_at", columnList = "searched_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class SearchHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "query_text", length = 255)
    private String queryText;

    @Column(name = "district", length = 100)
    private String district;

    @Column(name = "min_price")
    private Double minPrice;

    @Column(name = "max_price")
    private Double maxPrice;

    @CreatedDate
    @Column(name = "searched_at", nullable = false, updatable = false)
    private Instant searchedAt;
}
