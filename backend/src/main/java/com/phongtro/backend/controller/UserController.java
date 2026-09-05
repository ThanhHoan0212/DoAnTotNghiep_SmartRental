package com.phongtro.backend.controller;

import com.phongtro.backend.common.ApiResponse;
import com.phongtro.backend.common.AppConstants;
import com.phongtro.backend.common.PageResponse;
import com.phongtro.backend.dto.request.ChangePasswordRequest;
import com.phongtro.backend.dto.request.UpdateUserRequest;
import com.phongtro.backend.dto.request.UpdateUserStatusRequest;
import com.phongtro.backend.dto.response.UserResponse;
import com.phongtro.backend.entity.Role;
import com.phongtro.backend.entity.UserStatus;
import com.phongtro.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "2. User Management", description = "Quản lý hồ sơ người dùng cá nhân và chức năng quản trị viên (Admin)")
@SecurityRequirement(name = "BearerAuth")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Lấy thông tin cá nhân", description = "Lấy hồ sơ chi tiết của người dùng đang đăng nhập")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(Authentication authentication) {
        UserResponse response = userService.getCurrentUserProfile(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/me")
    @Operation(summary = "Cập nhật thông tin cá nhân", description = "Cập nhật họ tên, số điện thoại, avatar của người dùng hiện tại")
    public ResponseEntity<ApiResponse<UserResponse>> updateCurrentUser(
            Authentication authentication,
            @Valid @RequestBody UpdateUserRequest request) {
        UserResponse response = userService.updateUserProfile(authentication.getName(), request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thông tin thành công", response));
    }

    @PutMapping("/me/change-password")
    @Operation(summary = "Đổi mật khẩu tài khoản", description = "Người dùng tự đổi mật khẩu sau khi cung cấp đúng mật khẩu cũ")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(authentication.getName(), request);
        return ResponseEntity.ok(ApiResponse.success("Đổi mật khẩu thành công", null));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[Admin] Lấy danh sách tất cả người dùng", description = "Phân trang danh sách người dùng với bộ lọc theo vai trò (Role) và trạng thái (Status)")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getAllUsers(
            @Parameter(description = "Số trang (bắt đầu từ 0)")
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @Parameter(description = "Số bản ghi mỗi trang")
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size,
            @Parameter(description = "Lọc theo vai trò người dùng (TENANT, LANDLORD, ADMIN)")
            @RequestParam(required = false) Role role,
            @Parameter(description = "Lọc theo trạng thái tài khoản (ACTIVE, INACTIVE, BANNED)")
            @RequestParam(required = false) UserStatus status) {

        PageResponse<UserResponse> response = userService.getAllUsers(page, size, role, status);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[Admin] Cập nhật trạng thái người dùng", description = "Admin khóa (BANNED) hoặc kích hoạt lại (ACTIVE) tài khoản người dùng")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserStatusRequest request) {
        UserResponse response = userService.updateUserStatus(id, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái tài khoản thành công", response));
    }
}
