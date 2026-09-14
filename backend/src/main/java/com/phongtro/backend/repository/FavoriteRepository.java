package com.phongtro.backend.repository;

import com.phongtro.backend.entity.Favorite;
import com.phongtro.backend.entity.Room;
import com.phongtro.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, UUID> {

    Optional<Favorite> findByUserAndRoom(User user, Room room);

    boolean existsByUserAndRoom(User user, Room room);

    boolean existsByUserIdAndRoomId(UUID userId, UUID roomId);

    @Query("SELECT f.room FROM Favorite f WHERE f.user = :user ORDER BY f.createdAt DESC")
    Page<Room> findFavoriteRoomsByUser(@Param("user") User user, Pageable pageable);

    @Query("SELECT f.room.id FROM Favorite f WHERE f.user = :user")
    java.util.List<UUID> findFavoriteRoomIdsByUser(@Param("user") User user);

    @Modifying
    @Query("DELETE FROM Favorite f WHERE f.user = :user AND f.room = :room")
    void deleteByUserAndRoom(@Param("user") User user, @Param("room") Room room);

    long countByRoom(Room room);
}
