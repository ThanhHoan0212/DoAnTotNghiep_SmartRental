package com.phongtro.backend.repository;

import com.phongtro.backend.entity.Room;
import com.phongtro.backend.entity.RoomImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RoomImageRepository extends JpaRepository<RoomImage, UUID> {

    List<RoomImage> findByRoom(Room room);
}
