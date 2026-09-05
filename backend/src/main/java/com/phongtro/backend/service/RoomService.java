package com.phongtro.backend.service;

import com.phongtro.backend.common.PageResponse;
import com.phongtro.backend.dto.request.CreateRoomRequest;
import com.phongtro.backend.dto.request.UpdateRoomRequest;
import com.phongtro.backend.dto.response.RoomResponse;
import com.phongtro.backend.dto.response.RoomSummaryResponse;
import com.phongtro.backend.entity.RoomStatus;

import java.util.UUID;

public interface RoomService {

    RoomResponse createRoom(CreateRoomRequest request, String landlordEmail);

    RoomResponse updateRoom(UUID roomId, UpdateRoomRequest request, String userEmail);

    void deleteRoom(UUID roomId, String userEmail);

    RoomResponse getRoomDetail(UUID roomId, boolean incrementView);

    PageResponse<RoomSummaryResponse> getApprovedRooms(
            int page,
            int size,
            String district,
            Double minPrice,
            Double maxPrice,
            Long amenityId
    );

    PageResponse<RoomSummaryResponse> getMyRooms(String landlordEmail, int page, int size, RoomStatus status);

    PageResponse<RoomSummaryResponse> getAllRoomsForAdmin(int page, int size, RoomStatus status, String district);

    RoomResponse updateRoomStatus(UUID roomId, RoomStatus status);
}
