package com.phongtro.backend.controller;

import com.phongtro.backend.common.ApiResponse;
import com.phongtro.backend.common.AppConstants;
import com.phongtro.backend.common.PageResponse;
import com.phongtro.backend.dto.request.CreateRoomRequest;
import com.phongtro.backend.dto.request.UpdateRoomRequest;
import com.phongtro.backend.dto.request.UpdateRoomStatusRequest;
import com.phongtro.backend.dto.response.RoomResponse;
import com.phongtro.backend.dto.response.RoomSummaryResponse;
import com.phongtro.backend.entity.RoomStatus;
import com.phongtro.backend.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
@Tag(name = "4. Room Listing Management", description = "Quản lý tin đăng phòng trọ, tìm kiếm, đăng tin và kiểm duyệt")
public class RoomController {

    private final RoomService roomService;

    @GetMapping
    @Operation(summary = "Lấy danh sách phòng trọ đã được duyệt", description = "API công khai hỗ trợ phân trang và lọc theo quận/huyện, khoảng giá, tiện ích")
    public ResponseEntity<ApiResponse<PageResponse<RoomSummaryResponse>>> getApprovedRooms(
            @Parameter(description = "Số trang (bắt đầu từ 0)")
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @Parameter(description = "Số lượng bản ghi mỗi trang")
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size,
            @Parameter(description = "Lọc theo tên quận/huyện (vd: Cầu Giấy, Đống Đa...)")
            @RequestParam(required = false) String district,
            @Parameter(description = "Giá thuê tối thiểu (VNĐ)")
            @RequestParam(required = false) Double minPrice,
            @Parameter(description = "Giá thuê tối đa (VNĐ)")
            @RequestParam(required = false) Double maxPrice,
            @Parameter(description = "Lọc theo ID tiện ích cụ thể")
            @RequestParam(required = false) Long amenityId) {

        PageResponse<RoomSummaryResponse> response = roomService.getApprovedRooms(
                page, size, district, minPrice, maxPrice, amenityId
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết phòng trọ", description = "API công khai xem toàn bộ thông tin phòng, tiện ích, hình ảnh, thông tin chủ nhà và tự động tăng lượt xem (view count)")
    public ResponseEntity<ApiResponse<RoomResponse>> getRoomDetail(@PathVariable UUID id) {
        RoomResponse response = roomService.getRoomDetail(id, true);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('LANDLORD', 'ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Đăng tin phòng trọ mới", description = "Dành cho Chủ nhà (LANDLORD) hoặc Admin đăng tin phòng trọ mới kèm hình ảnh và tiện ích")
    public ResponseEntity<ApiResponse<RoomResponse>> createRoom(
            Authentication authentication,
            @Valid @RequestBody CreateRoomRequest request) {
        RoomResponse response = roomService.createRoom(request, authentication.getName());
        return new ResponseEntity<>(
                ApiResponse.success("Đăng tin phòng trọ thành công", response),
                HttpStatus.CREATED
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('LANDLORD', 'ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Cập nhật tin đăng phòng trọ", description = "Chủ nhà sở hữu tin đăng hoặc Admin có quyền chỉnh sửa nội dung, giá, ảnh, tiện ích")
    public ResponseEntity<ApiResponse<RoomResponse>> updateRoom(
            Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRoomRequest request) {
        RoomResponse response = roomService.updateRoom(id, request, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Cập nhật tin đăng thành công", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('LANDLORD', 'ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Xóa tin đăng phòng trọ", description = "Chủ nhà sở hữu hoặc Admin xóa tin đăng")
    public ResponseEntity<ApiResponse<Void>> deleteRoom(
            Authentication authentication,
            @PathVariable UUID id) {
        roomService.deleteRoom(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Xóa tin đăng phòng trọ thành công", null));
    }

    @GetMapping("/my-rooms")
    @PreAuthorize("hasAnyRole('LANDLORD', 'ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Danh sách tin đăng của tôi", description = "Lấy toàn bộ tin đăng của Chủ nhà đang đăng nhập kèm phân trang và lọc theo trạng thái")
    public ResponseEntity<ApiResponse<PageResponse<RoomSummaryResponse>>> getMyRooms(
            Authentication authentication,
            @Parameter(description = "Số trang (bắt đầu từ 0)")
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @Parameter(description = "Số lượng bản ghi mỗi trang")
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size,
            @Parameter(description = "Lọc theo trạng thái (PENDING, APPROVED, REJECTED, HIDDEN)")
            @RequestParam(required = false) RoomStatus status) {

        PageResponse<RoomSummaryResponse> response = roomService.getMyRooms(
                authentication.getName(), page, size, status
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "[Admin] Lấy tất cả tin đăng trong hệ thống", description = "Quản trị viên xem danh sách toàn bộ tin đăng để duyệt hoặc quản lý")
    public ResponseEntity<ApiResponse<PageResponse<RoomSummaryResponse>>> getAllRoomsForAdmin(
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size,
            @RequestParam(required = false) RoomStatus status,
            @RequestParam(required = false) String district) {

        PageResponse<RoomSummaryResponse> response = roomService.getAllRoomsForAdmin(page, size, status, district);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "[Admin] Duyệt hoặc thay đổi trạng thái tin đăng", description = "Quản trị viên duyệt (APPROVED), từ chối (REJECTED) hoặc ẩn (HIDDEN) tin đăng")
    public ResponseEntity<ApiResponse<RoomResponse>> updateRoomStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRoomStatusRequest request) {
        RoomResponse response = roomService.updateRoomStatus(id, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái tin đăng thành công", response));
    }
}
