package com.phongtro.backend.repository;

import com.phongtro.backend.entity.Room;
import com.phongtro.backend.entity.RoomStatus;
import com.phongtro.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoomRepository extends JpaRepository<Room, UUID>, JpaSpecificationExecutor<Room> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Room r WHERE r.id = :id")
    Optional<Room> findByIdForUpdate(@Param("id") UUID id);

    Page<Room> findByStatus(RoomStatus status, Pageable pageable);

    Page<Room> findByLandlord(User landlord, Pageable pageable);

    Page<Room> findByLandlordAndStatus(User landlord, RoomStatus status, Pageable pageable);

    @Query("SELECT r FROM Room r " +
            "LEFT JOIN FETCH r.images " +
            "LEFT JOIN FETCH r.amenities " +
            "WHERE r.id = :id")
    Optional<Room> findByIdWithDetails(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE Room r SET r.viewCount = r.viewCount + 1 WHERE r.id = :id")
    int incrementViewCount(@Param("id") UUID id);

    @Query("SELECT new com.phongtro.backend.dto.response.DistrictCountResponse(r.district, COUNT(r)) " +
            "FROM Room r " +
            "WHERE r.status IN (com.phongtro.backend.entity.RoomStatus.APPROVED, com.phongtro.backend.entity.RoomStatus.RESERVED, com.phongtro.backend.entity.RoomStatus.RENTED) AND r.district IS NOT NULL " +
            "GROUP BY r.district ORDER BY COUNT(r) DESC")
    java.util.List<com.phongtro.backend.dto.response.DistrictCountResponse> countRoomsByDistrict();
}
