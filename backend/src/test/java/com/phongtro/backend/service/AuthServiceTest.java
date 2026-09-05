package com.phongtro.backend.service;

import com.phongtro.backend.dto.request.LoginRequest;
import com.phongtro.backend.dto.request.RefreshTokenRequest;
import com.phongtro.backend.dto.request.RegisterRequest;
import com.phongtro.backend.dto.response.AuthResponse;
import com.phongtro.backend.dto.response.TokenRefreshResponse;
import com.phongtro.backend.dto.response.UserResponse;
import com.phongtro.backend.entity.RefreshToken;
import com.phongtro.backend.entity.Role;
import com.phongtro.backend.entity.User;
import com.phongtro.backend.entity.UserStatus;
import com.phongtro.backend.exception.AppException;
import com.phongtro.backend.exception.ErrorCode;
import com.phongtro.backend.mapper.UserMapper;
import com.phongtro.backend.repository.UserRepository;
import com.phongtro.backend.security.JwtTokenProvider;
import com.phongtro.backend.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthServiceImpl authService;

    private User sampleUser;
    private UserResponse sampleUserResponse;
    private RefreshToken sampleRefreshToken;

    @BeforeEach
    void setUp() {
        UUID userId = UUID.randomUUID();
        sampleUser = User.builder()
                .id(userId)
                .email("test@phongtro.vn")
                .passwordHash("encodedPassword123")
                .fullName("Nguyễn Văn Test")
                .phone("0987654321")
                .role(Role.TENANT)
                .status(UserStatus.ACTIVE)
                .build();

        sampleUserResponse = UserResponse.builder()
                .id(userId)
                .email("test@phongtro.vn")
                .fullName("Nguyễn Văn Test")
                .phone("0987654321")
                .role(Role.TENANT)
                .status(UserStatus.ACTIVE)
                .build();

        sampleRefreshToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .token("mock-refresh-token-uuid")
                .expiryDate(Instant.now().plusSeconds(604800))
                .revoked(false)
                .build();
    }

    @Test
    @DisplayName("Đăng ký thành công - trả về thông tin người dùng và JWT")
    void register_Success() {
        RegisterRequest request = RegisterRequest.builder()
                .email("test@phongtro.vn")
                .password("Password@123")
                .fullName("Nguyễn Văn Test")
                .phone("0987654321")
                .role(Role.TENANT)
                .build();

        when(userRepository.existsByEmail("test@phongtro.vn")).thenReturn(false);
        when(userRepository.existsByPhone("0987654321")).thenReturn(false);
        when(passwordEncoder.encode("Password@123")).thenReturn("encodedPassword123");
        when(userMapper.toEntity(eq(request), eq("encodedPassword123"))).thenReturn(sampleUser);
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(jwtTokenProvider.generateAccessToken(eq(sampleUser.getId()), eq(sampleUser.getEmail()), eq("TENANT")))
                .thenReturn("mock-access-token");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(86400000L);
        when(refreshTokenService.createRefreshToken(any(User.class))).thenReturn(sampleRefreshToken);
        when(userMapper.toUserResponse(any(User.class))).thenReturn(sampleUserResponse);

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("mock-access-token", response.getAccessToken());
        assertEquals("mock-refresh-token-uuid", response.getRefreshToken());
        assertEquals("test@phongtro.vn", response.getUser().getEmail());

        verify(emailService, times(1)).sendWelcomeEmail(sampleUser.getEmail(), sampleUser.getFullName());
        verify(userRepository, times(1)).save(sampleUser);
    }

    @Test
    @DisplayName("Đăng ký thất bại khi email đã tồn tại - Ném AppException USER_ALREADY_EXISTS")
    void register_EmailAlreadyExists_ThrowsException() {
        RegisterRequest request = RegisterRequest.builder()
                .email("test@phongtro.vn")
                .password("Password@123")
                .fullName("Nguyễn Văn Test")
                .build();

        when(userRepository.existsByEmail("test@phongtro.vn")).thenReturn(true);

        AppException exception = assertThrows(AppException.class, () -> authService.register(request));
        assertEquals(ErrorCode.USER_ALREADY_EXISTS, exception.getErrorCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Đăng nhập thành công - trả về cặp token")
    void login_Success() {
        LoginRequest request = LoginRequest.builder()
                .email("test@phongtro.vn")
                .password("Password@123")
                .build();

        when(userRepository.findByEmail("test@phongtro.vn")).thenReturn(Optional.of(sampleUser));
        when(jwtTokenProvider.generateAccessToken(eq(sampleUser.getId()), eq(sampleUser.getEmail()), eq("TENANT")))
                .thenReturn("mock-access-token");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(86400000L);
        when(refreshTokenService.createRefreshToken(sampleUser)).thenReturn(sampleRefreshToken);
        when(userMapper.toUserResponse(sampleUser)).thenReturn(sampleUserResponse);

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mock-access-token", response.getAccessToken());
        assertEquals("mock-refresh-token-uuid", response.getRefreshToken());
        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("Đăng nhập thất bại do sai mật khẩu - Ném AppException BAD_CREDENTIALS")
    void login_BadCredentials_ThrowsException() {
        LoginRequest request = LoginRequest.builder()
                .email("test@phongtro.vn")
                .password("WrongPassword")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Sai thông tin đăng nhập"));

        AppException exception = assertThrows(AppException.class, () -> authService.login(request));
        assertEquals(ErrorCode.BAD_CREDENTIALS, exception.getErrorCode());
    }

    @Test
    @DisplayName("Đăng nhập thất bại khi tài khoản bị khóa - Ném AppException USER_BANNED")
    void login_UserBanned_ThrowsException() {
        sampleUser.setStatus(UserStatus.BANNED);

        LoginRequest request = LoginRequest.builder()
                .email("test@phongtro.vn")
                .password("Password@123")
                .build();

        when(userRepository.findByEmail("test@phongtro.vn")).thenReturn(Optional.of(sampleUser));

        AppException exception = assertThrows(AppException.class, () -> authService.login(request));
        assertEquals(ErrorCode.USER_BANNED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Làm mới token thành công - cấp Access Token mới")
    void refreshToken_Success() {
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("mock-refresh-token-uuid")
                .build();

        when(refreshTokenService.findByToken("mock-refresh-token-uuid")).thenReturn(Optional.of(sampleRefreshToken));
        when(refreshTokenService.verifyExpiration(sampleRefreshToken)).thenReturn(sampleRefreshToken);
        when(jwtTokenProvider.generateAccessToken(eq(sampleUser.getId()), eq(sampleUser.getEmail()), eq("TENANT")))
                .thenReturn("new-mock-access-token");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(86400000L);

        TokenRefreshResponse response = authService.refreshToken(request);

        assertNotNull(response);
        assertEquals("new-mock-access-token", response.getAccessToken());
        assertEquals("mock-refresh-token-uuid", response.getRefreshToken());
    }

    @Test
    @DisplayName("Làm mới token thất bại khi token không tồn tại - Ném AppException REFRESH_TOKEN_NOT_FOUND")
    void refreshToken_NotFound_ThrowsException() {
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("non-existent-token")
                .build();

        when(refreshTokenService.findByToken("non-existent-token")).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> authService.refreshToken(request));
        assertEquals(ErrorCode.REFRESH_TOKEN_NOT_FOUND, exception.getErrorCode());
    }
}
