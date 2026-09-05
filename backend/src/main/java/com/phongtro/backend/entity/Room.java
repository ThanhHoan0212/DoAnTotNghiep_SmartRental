package com.phongtro.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(
        name = "rooms",
        indexes = {
                @Index(name = "idx_rooms_landlord_id", columnList = "landlord_id"),
                @Index(name = "idx_rooms_status", columnList = "status"),
                @Index(name = "idx_rooms_district", columnList = "district"),
                @Index(name = "idx_rooms_price", columnList = "price"),
                @Index(name = "idx_rooms_area", columnList = "area")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Room extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "landlord_id", nullable = false)
    private User landlord;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "price", nullable = false)
    private Double price;

    @Column(name = "area", nullable = false)
    private Double area;

    @Column(name = "address", nullable = false, length = 255)
    private String address;

    @Column(name = "ward", length = 100)
    private String ward;

    @Column(name = "district", nullable = false, length = 100)
    private String district;

    @Column(name = "city", nullable = false, length = 100)
    @Builder.Default
    private String city = "Hà Nội";

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private RoomStatus status = RoomStatus.PENDING;

    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private Long viewCount = 0L;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RoomImage> images = new ArrayList<>();

    @ManyToMany
    @JoinTable(
            name = "room_amenities",
            joinColumns = @JoinColumn(name = "room_id"),
            inverseJoinColumns = @JoinColumn(name = "amenity_id")
    )
    @Builder.Default
    private Set<Amenity> amenities = new HashSet<>();

    public void addImage(RoomImage image) {
        images.add(image);
        image.setRoom(this);
    }

    public void removeImage(RoomImage image) {
        images.remove(image);
        image.setRoom(null);
    }

    public void clearImages() {
        images.forEach(img -> img.setRoom(null));
        images.clear();
    }
}
