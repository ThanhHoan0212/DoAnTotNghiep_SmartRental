package com.phongtro.backend.service;

import com.phongtro.backend.dto.request.ForgotPasswordRequest;
import com.phongtro.backend.dto.request.LoginRequest;
import com.phongtro.backend.dto.request.RefreshTokenRequest;
import com.phongtro.backend.dto.request.RegisterRequest;
import com.phongtro.backend.dto.request.ResetPasswordRequest;
import com.phongtro.backend.dto.response.AuthResponse;
import com.phongtro.backend.dto.response.TokenRefreshResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    TokenRefreshResponse refreshToken(RefreshTokenRequest request);

    void logout(String refreshToken);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);
}
