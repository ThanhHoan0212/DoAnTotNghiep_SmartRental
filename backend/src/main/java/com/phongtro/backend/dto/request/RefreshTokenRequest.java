package com.phongtro.backend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu cấp mới access token bằng refresh token")
public class RefreshTokenRequest {

    @NotBlank(message = "Refresh token không được để trống")
    @Schema(description = "Refresh token hợp lệ đã được cấp khi đăng nhập", example = "d87a4087-94d0-4bf6-b51e-...")
    private String refreshToken;
}
