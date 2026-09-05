package com.phongtro.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "amenities",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_amenities_name", columnNames = "name")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Amenity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "icon", length = 50)
    private String icon;
}
