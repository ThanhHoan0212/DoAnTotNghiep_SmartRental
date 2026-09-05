package com.phongtro.backend.service.impl;

import com.phongtro.backend.dto.request.ForgotPasswordRequest;
import com.phongtro.backend.dto.request.LoginRequest;
import com.phongtro.backend.dto.request.RefreshTokenRequest;
import com.phongtro.backend.dto.request.RegisterRequest;
import com.phongtro.backend.dto.request.ResetPasswordRequest;
import com.phongtro.backend.dto.response.AuthResponse;
import com.phongtro.backend.dto.response.TokenRefreshResponse;
import com.phongtro.backend.dto.response.UserResponse;
import com.phongtro.backend.entity.RefreshToken;
import com.phongtro.backend.entity.User;
import com.phongtro.backend.entity.UserStatus;
import com.phongtro.backend.exception.AppException;
import com.phongtro.backend.exception.ErrorCode;
import com.phongtro.backend.mapper.UserMapper;
import com.phongtro.backend.repository.UserRepository;
import com.phongtro.backend.security.JwtTokenProvider;
import com.phongtro.backend.service.AuthService;
import com.phongtro.backend.service.EmailService;
import com.phongtro.backend.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    // Lưu trữ tạm thời mã OTP reset password trong bộ nhớ cho đồ án (token -> email)
    private final Map<String, String> resetPasswordTokens = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        // 1. Kiểm tra trùng lặp email
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new AppException(ErrorCode.USER_ALREADY_EXISTS);
        }

        // 2. Kiểm tra trùng lặp số điện thoại
        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            if (userRepository.existsByPhone(request.getPhone().trim())) {
                throw new AppException(ErrorCode.PHONE_ALREADY_EXISTS);
            }
        }

        // 3. Mã hóa mật khẩu và lưu Entity
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User user = userMapper.toEntity(request, encodedPassword);
        User savedUser = userRepository.save(user);

        // 4. Gửi email chào mừng (mock)
        emailService.sendWelcomeEmail(savedUser.getEmail(), savedUser.getFullName());

        // 5. Sinh cặp JWT Access Token và Refresh Token
        String accessToken = jwtTokenProvider.generateAccessToken(
                savedUser.getId(), savedUser.getEmail(), savedUser.getRole().name()
        );
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(savedUser);

        UserResponse userResponse = userMapper.toUserResponse(savedUser);

        log.info("User registered successfully with email: [{}]", normalizedEmail);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationMs())
                .user(userResponse)
                .build();
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        // 1. Xác thực bằng AuthenticationManager
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword())
            );
        } catch (BadCredentialsException e) {
            throw new AppException(ErrorCode.BAD_CREDENTIALS);
        }

        // 2. Lấy thông tin người dùng từ Database
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // 3. Kiểm tra trạng thái tài khoản
        if (user.getStatus() == UserStatus.BANNED) {
            throw new AppException(ErrorCode.USER_BANNED);
        }
        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new AppException(ErrorCode.USER_INACTIVE);
        }

        // 4. Sinh JWT Access Token & Refresh Token
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getEmail(), user.getRole().name()
        );
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        UserResponse userResponse = userMapper.toUserResponse(user);

        log.info("User logged in successfully: [{}]", normalizedEmail);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationMs())
                .user(userResponse)
                .build();
    }

    @Override
    @Transactional
    public TokenRefreshResponse refreshToken(RefreshTokenRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        return refreshTokenService.findByToken(requestRefreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    if (user.getStatus() == UserStatus.BANNED) {
                        throw new AppException(ErrorCode.USER_BANNED);
                    }
                    if (user.getStatus() == UserStatus.INACTIVE) {
                        throw new AppException(ErrorCode.USER_INACTIVE);
                    }

                    String newAccessToken = jwtTokenProvider.generateAccessToken(
                            user.getId(), user.getEmail(), user.getRole().name()
                    );

                    log.info("Access token refreshed successfully for user: [{}]", user.getEmail());

                    return TokenRefreshResponse.builder()
                            .accessToken(newAccessToken)
                            .refreshToken(requestRefreshToken)
                            .tokenType("Bearer")
                            .expiresIn(jwtTokenProvider.getExpirationMs())
                            .build();
                })
                .orElseThrow(() -> new AppException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenService.revokeToken(refreshToken);
            log.info("Refresh token revoked on logout: [{}]", refreshToken);
        }
    }

    @Override
    public void forgotPassword(ForgotPasswordRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        // Kiểm tra xem email có tồn tại không
        userRepository.findByEmail(normalizedEmail).ifPresent(user -> {
            // Tạo mã OTP 6 chữ số ngẫu nhiên
            String otpCode = String.format("%06d", (int) (Math.random() * 1000000));
            resetPasswordTokens.put(otpCode, normalizedEmail);
            emailService.sendPasswordResetEmail(normalizedEmail, otpCode);
            log.info("Generated password reset OTP for email: [{}]", normalizedEmail);
        });
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String email = resetPasswordTokens.get(request.getToken());

        if (email == null) {
            throw new AppException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID);
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Hủy token sau khi đã sử dụng
        resetPasswordTokens.remove(request.getToken());
        // Thu hồi toàn bộ refresh token cũ để buộc đăng nhập lại
        refreshTokenService.revokeUserTokens(user);

        log.info("Password successfully reset for user: [{}]", email);
    }
}
