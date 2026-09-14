package com.phongtro.backend.controller;

import com.phongtro.backend.common.ApiResponse;
import com.phongtro.backend.common.AppConstants;
import com.phongtro.backend.common.PageResponse;
import com.phongtro.backend.dto.response.FavoriteStatusResponse;
import com.phongtro.backend.dto.response.RoomSummaryResponse;
import com.phongtro.backend.dto.response.ToggleFavoriteResponse;
import com.phongtro.backend.service.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/favorites")
@RequiredArgsConstructor
@Tag(name = "7. Favorites Management", description = "Quản lý danh sách phòng trọ yêu thích của người dùng")
public class FavoriteController {

    private final FavoriteService favoriteService;

    @PostMapping("/{roomId}/toggle")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Lưu hoặc hủy lưu phòng trọ yêu thích (Toggle)",
            description = "Nếu phòng chưa được lưu -> Thêm vào yêu thích. Nếu đã lưu -> Hủy lưu và xóa khỏi danh sách.")
    public ResponseEntity<ApiResponse<ToggleFavoriteResponse>> toggleFavorite(
            Authentication authentication,
            @PathVariable UUID roomId) {
        ToggleFavoriteResponse response = favoriteService.toggleFavorite(roomId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response));
    }

    @GetMapping("/{roomId}/status")
    @Operation(summary = "Kiểm tra trạng thái yêu thích của phòng trọ",
            description = "Trả về trạng thái người dùng hiện tại đã lưu phòng này hay chưa và tổng số lượt lưu của phòng")
    public ResponseEntity<ApiResponse<FavoriteStatusResponse>> checkFavoriteStatus(
            Authentication authentication,
            @PathVariable UUID roomId) {
        String userEmail = authentication != null && authentication.isAuthenticated() && !authentication.getName().equals("anonymousUser")
                ? authentication.getName()
                : null;

        FavoriteStatusResponse response = favoriteService.checkFavoriteStatus(roomId, userEmail);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/my-favorites")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Lấy danh sách các phòng trọ tôi đã lưu yêu thích",
            description = "Trả về danh sách phòng trọ đã lưu của tài khoản hiện tại kèm phân trang")
    public ResponseEntity<ApiResponse<PageResponse<RoomSummaryResponse>>> getMyFavoriteRooms(
            Authentication authentication,
            @Parameter(description = "Số trang (bắt đầu từ 0)")
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @Parameter(description = "Số lượng bản ghi mỗi trang")
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size) {
        PageResponse<RoomSummaryResponse> response = favoriteService.getMyFavoriteRooms(
                authentication.getName(), page, size
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/my-favorite-ids")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Lấy danh sách ID các phòng trọ tôi đã lưu yêu thích",
            description = "Trả về mảng UUID của tất cả các phòng trọ mà người dùng hiện tại đã bấm thích")
    public ResponseEntity<ApiResponse<java.util.List<UUID>>> getMyFavoriteRoomIds(
            Authentication authentication) {
        java.util.List<UUID> ids = favoriteService.getMyFavoriteRoomIds(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(ids));
    }

    @DeleteMapping("/{roomId}")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Xóa phòng trọ khỏi danh sách yêu thích",
            description = "Hủy yêu thích một phòng trọ cụ thể")
    public ResponseEntity<ApiResponse<Void>> removeFavorite(
            Authentication authentication,
            @PathVariable UUID roomId) {
        favoriteService.removeFavorite(roomId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Đã xóa phòng trọ khỏi danh sách yêu thích", null));
    }
}
