package com.phongtro.backend.dto.response;

import com.phongtro.backend.entity.Role;
import com.phongtro.backend.entity.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thông tin chi tiết người dùng")
public class UserResponse {

    @Schema(description = "Mã định danh duy nhất (UUID)", example = "b1b60167-2705-4fbf-9310-f1c5040d3478")
    private UUID id;

    @Schema(description = "Địa chỉ email", example = "nguyenvana@gmail.com")
    private String email;

    @Schema(description = "Họ và tên", example = "Nguyễn Văn A")
    private String fullName;

    @Schema(description = "Số điện thoại", example = "0987654321")
    private String phone;

    @Schema(description = "Đường dẫn ảnh đại diện", example = "https://example.com/avatar.jpg")
    private String avatarUrl;

    @Schema(description = "Vai trò người dùng", example = "TENANT")
    private Role role;

    @Schema(description = "Trạng thái tài khoản", example = "ACTIVE")
    private UserStatus status;

    @Schema(description = "Thời gian tạo tài khoản")
    private Instant createdAt;

    @Schema(description = "Thời gian cập nhật gần nhất")
    private Instant updatedAt;
}
