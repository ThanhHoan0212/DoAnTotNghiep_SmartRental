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
@Schema(description = "Phản hồi cấp mới Access Token thành công")
public class TokenRefreshResponse {

    @Schema(description = "Access Token mới", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;

    @Schema(description = "Refresh Token (có thể xoay vòng mới)", example = "d87a4087-94d0-4bf6-b51e-...")
    private String refreshToken;

    @Schema(description = "Loại token", example = "Bearer", defaultValue = "Bearer")
    @Builder.Default
    private String tokenType = "Bearer";

    @Schema(description = "Thời gian hết hạn của Access Token tính bằng mili-giây", example = "86400000")
    private long expiresIn;
}
