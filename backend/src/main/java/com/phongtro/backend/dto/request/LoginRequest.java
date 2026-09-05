package com.phongtro.backend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu đăng nhập hệ thống")
public class LoginRequest {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    @Schema(description = "Địa chỉ email đăng nhập", example = "nguyenvana@gmail.com")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Schema(description = "Mật khẩu người dùng", example = "Password@123")
    private String password;
}
