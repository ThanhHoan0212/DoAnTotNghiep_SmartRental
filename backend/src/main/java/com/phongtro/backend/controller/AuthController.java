package com.phongtro.backend.controller;

import com.phongtro.backend.common.ApiResponse;
import com.phongtro.backend.dto.request.ForgotPasswordRequest;
import com.phongtro.backend.dto.request.LoginRequest;
import com.phongtro.backend.dto.request.RefreshTokenRequest;
import com.phongtro.backend.dto.request.RegisterRequest;
import com.phongtro.backend.dto.request.ResetPasswordRequest;
import com.phongtro.backend.dto.response.AuthResponse;
import com.phongtro.backend.dto.response.TokenRefreshResponse;
import com.phongtro.backend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "1. Authentication", description = "Quản lý đăng ký, đăng nhập, JWT access token, refresh token và quên mật khẩu")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Đăng ký tài khoản người dùng mới", description = "Cho phép người thuê (TENANT) hoặc chủ nhà (LANDLORD) tạo tài khoản mới")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return new ResponseEntity<>(
                ApiResponse.success("Đăng ký tài khoản thành công", response),
                HttpStatus.CREATED
        );
    }

    @PostMapping("/login")
    @Operation(summary = "Đăng nhập hệ thống", description = "Xác thực email và mật khẩu, trả về cặp JWT Access Token và Refresh Token")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", response));
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Làm mới Access Token", description = "Sử dụng Refresh Token hợp lệ để cấp mới Access Token mà không cần đăng nhập lại")
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        TokenRefreshResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success("Cấp mới token thành công", response));
    }

    @PostMapping("/logout")
    @Operation(summary = "Đăng xuất tài khoản", description = "Thu hồi Refresh Token để vô hiệu hóa phiên đăng nhập")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestBody(required = false) RefreshTokenRequest request) {
        if (request != null && request.getRefreshToken() != null) {
            authService.logout(request.getRefreshToken());
        }
        return ResponseEntity.ok(ApiResponse.success("Đăng xuất thành công", null));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Yêu cầu khôi phục mật khẩu", description = "Gửi mã OTP hoặc liên kết đặt lại mật khẩu về email người dùng")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Nếu email tồn tại trong hệ thống, mã xác nhận đã được gửi đi", null));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Đặt lại mật khẩu mới", description = "Sử dụng mã OTP/Token nhận được qua email để đặt mật khẩu mới")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Đặt lại mật khẩu mới thành công", null));
    }
}
