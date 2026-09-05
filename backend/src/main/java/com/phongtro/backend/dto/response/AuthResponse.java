package com.phongtro.backend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thông tin phản hồi sau khi xác thực thành công")
public class AuthResponse {

    @Schema(description = "JWT Access Token dùng để truy cập các API yêu cầu xác thực", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;

    @Schema(description = "Refresh Token dùng để xin cấp lại Access Token mới", example = "4fc9a909-6447-490b-93df-520e58ec8f6c")
    private String refreshToken;

    @Schema(description = "Loại token xác thực", example = "Bearer", defaultValue = "Bearer")
    @Builder.Default
    private String tokenType = "Bearer";

    @Schema(description = "Thời gian hết hạn của Access Token tính bằng mili-giây", example = "86400000")
    private long expiresIn;

    @Schema(description = "Thông tin tóm tắt người dùng đã đăng nhập")
    private UserResponse user;
}
