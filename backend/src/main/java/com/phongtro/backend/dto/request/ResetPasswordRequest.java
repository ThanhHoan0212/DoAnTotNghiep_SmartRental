package com.phongtro.backend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu đặt lại mật khẩu với token hoặc mã OTP xác thực")
public class ResetPasswordRequest {

    @NotBlank(message = "Mã xác thực không được để trống")
    @Schema(description = "Token hoặc mã OTP nhận được qua email", example = "123456")
    private String token;

    @NotBlank(message = "Mật khẩu mới không được để trống")
    @Size(min = 6, max = 50, message = "Mật khẩu mới phải từ 6 đến 50 ký tự")
    @Schema(description = "Mật khẩu mới", example = "NewSecret@123")
    private String newPassword;
}
