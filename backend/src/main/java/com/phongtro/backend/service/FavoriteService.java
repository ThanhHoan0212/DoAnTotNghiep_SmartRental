package com.phongtro.backend.service;

import com.phongtro.backend.common.PageResponse;
import com.phongtro.backend.dto.response.FavoriteStatusResponse;
import com.phongtro.backend.dto.response.RoomSummaryResponse;
import com.phongtro.backend.dto.response.ToggleFavoriteResponse;

import java.util.UUID;

public interface FavoriteService {

    /**
     * Thả tim / Hủy thích phòng trọ (Toggle Favorite)
     */
    ToggleFavoriteResponse toggleFavorite(UUID roomId, String userEmail);

    /**
     * Kiểm tra trạng thái người dùng hiện tại đã lưu phòng này hay chưa
     */
    FavoriteStatusResponse checkFavoriteStatus(UUID roomId, String userEmail);

    /**
     * Lấy danh sách các phòng trọ người dùng hiện tại đã lưu yêu thích (kèm phân trang)
     */
    PageResponse<RoomSummaryResponse> getMyFavoriteRooms(String userEmail, int page, int size);

    /**
     * Lấy danh sách ID các phòng trọ mà người dùng hiện tại đã lưu yêu thích
     */
    java.util.List<UUID> getMyFavoriteRoomIds(String userEmail);

    /**
     * Xóa một phòng trọ khỏi danh sách yêu thích
     */
    void removeFavorite(UUID roomId, String userEmail);
}
