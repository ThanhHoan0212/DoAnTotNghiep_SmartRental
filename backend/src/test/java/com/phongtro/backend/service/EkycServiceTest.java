package com.phongtro.backend.service;

import com.phongtro.backend.config.FptAiProperties;
import com.phongtro.backend.dto.ekyc.FptAiFaceMatchResult;
import com.phongtro.backend.dto.ekyc.FptAiOcrResult;
import com.phongtro.backend.dto.response.EkycStatusResponse;
import com.phongtro.backend.dto.response.EkycVerificationResponse;
import com.phongtro.backend.entity.EkycVerification;
import com.phongtro.backend.entity.Role;
import com.phongtro.backend.entity.User;
import com.phongtro.backend.entity.UserStatus;
import com.phongtro.backend.exception.AppException;
import com.phongtro.backend.exception.ErrorCode;
import com.phongtro.backend.repository.EkycVerificationRepository;
import com.phongtro.backend.repository.UserRepository;
import com.phongtro.backend.service.client.FptAiClient;
import com.phongtro.backend.service.impl.EkycServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EkycServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EkycVerificationRepository ekycVerificationRepository;

    @Mock
    private FptAiClient fptAiClient;

    @Mock
    private FptAiProperties fptAiProperties;

    @InjectMocks
    private EkycServiceImpl ekycService;

    private User sampleUser;
    private MockMultipartFile frontFile;
    private MockMultipartFile backFile;
    private MockMultipartFile selfieFile;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(UUID.randomUUID())
                .email("user@phongtro.vn")
                .fullName("Nguyễn Văn A")
                .role(Role.TENANT)
                .status(UserStatus.ACTIVE)
                .isIdentityVerified(false)
                .build();

        frontFile = new MockMultipartFile("frontImage", "front.jpg", "image/jpeg", "dummy-front-bytes".getBytes());
        backFile = new MockMultipartFile("backImage", "back.jpg", "image/jpeg", "dummy-back-bytes".getBytes());
        selfieFile = new MockMultipartFile("selfieImage", "selfie.jpg", "image/jpeg", "dummy-selfie-bytes".getBytes());

        lenient().when(fptAiProperties.getConfidenceThreshold()).thenReturn(85.0);
    }

    @Test
    @DisplayName("Xác thực eKYC thành công: Điểm tin cậy 92.5% > 85% -> Cấp chứng nhận định danh")
    void verifyIdentity_WhenConfidenceAbove85_ShouldSucceedAndMarkUserVerified() {
        FptAiOcrResult mockOcr = FptAiOcrResult.builder()
                .idCardNumber("079198001234")
                .fullName("NGUYỄN VĂN A")
                .dob("15/08/1998")
                .address("Phường 25, Bình Thạnh, TP.HCM")
                .hometown("TP. Hồ Chí Minh")
                .issueDate("10/05/2021")
                .cardType("chip_front")
                .rawJson("{}")
                .build();

        FptAiFaceMatchResult mockFaceMatch = FptAiFaceMatchResult.builder()
                .similarity(92.5)
                .isMatch(true)
                .rawJson("{}")
                .build();

        when(userRepository.findByEmail(sampleUser.getEmail())).thenReturn(Optional.of(sampleUser));
        when(fptAiClient.extractIdCard(frontFile, backFile)).thenReturn(mockOcr);
        when(fptAiClient.matchFaces(frontFile, selfieFile)).thenReturn(mockFaceMatch);
        when(ekycVerificationRepository.save(any(EkycVerification.class))).thenAnswer(i -> i.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        EkycVerificationResponse response = ekycService.verifyIdentity(
                frontFile, backFile, selfieFile, sampleUser.getEmail()
        );

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("SUCCESS", response.getStatus());
        assertEquals(92.5, response.getConfidenceScore());
        assertEquals("NGUYỄN VĂN A", response.getIdCardName());
        assertTrue(response.getIdCardNumberMasked().contains("******"));

        // Kiểm tra User entity đã được cập nhật
        assertTrue(sampleUser.isIdentityVerified());
        assertEquals("079198001234", sampleUser.getIdCardNumber());
        assertEquals(92.5, sampleUser.getEkycConfidenceScore());

        verify(ekycVerificationRepository, times(1)).save(any(EkycVerification.class));
        verify(userRepository, times(1)).save(sampleUser);
    }

    @Test
    @DisplayName("Xác thực eKYC thất bại: Điểm tin cậy 65.0% <= 85% -> Ghi nhận thất bại và ném ngoại lệ")
    void verifyIdentity_WhenConfidenceBelow85_ShouldRecordFailureAndThrowException() {
        FptAiOcrResult mockOcr = FptAiOcrResult.builder()
                .idCardNumber("079198001234")
                .fullName("NGUYỄN VĂN A")
                .dob("15/08/1998")
                .rawJson("{}")
                .build();

        FptAiFaceMatchResult mockFaceMatch = FptAiFaceMatchResult.builder()
                .similarity(65.0) // Thấp hơn 85%
                .isMatch(false)
                .rawJson("{}")
                .build();

        when(userRepository.findByEmail(sampleUser.getEmail())).thenReturn(Optional.of(sampleUser));
        when(fptAiClient.extractIdCard(frontFile, backFile)).thenReturn(mockOcr);
        when(fptAiClient.matchFaces(frontFile, selfieFile)).thenReturn(mockFaceMatch);

        AppException ex = assertThrows(AppException.class, () ->
                ekycService.verifyIdentity(frontFile, backFile, selfieFile, sampleUser.getEmail()));

        assertEquals(ErrorCode.EKYC_VERIFICATION_FAILED, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("chưa đạt ngưỡng tin cậy"));

        // Xác nhận đã lưu log thất bại vào database
        verify(ekycVerificationRepository, times(1)).save(argThat(v ->
                "FAILED".equals(v.getStatus()) && v.getFailureReason().contains("chưa đạt ngưỡng")
        ));
        // Xác nhận user KHÔNG được cấp chứng nhận
        assertFalse(sampleUser.isIdentityVerified());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Xác thực eKYC thất bại: Ảnh CCCD không nhận diện được số giấy tờ hợp lệ")
    void verifyIdentity_WhenOcrFailsToDetectIdNumber_ShouldThrowException() {
        FptAiOcrResult mockEmptyOcr = FptAiOcrResult.builder()
                .idCardNumber("") // Trống do ảnh mờ
                .rawJson("{}")
                .build();

        when(userRepository.findByEmail(sampleUser.getEmail())).thenReturn(Optional.of(sampleUser));
        when(fptAiClient.extractIdCard(frontFile, backFile)).thenReturn(mockEmptyOcr);

        AppException ex = assertThrows(AppException.class, () ->
                ekycService.verifyIdentity(frontFile, backFile, selfieFile, sampleUser.getEmail()));

        assertEquals(ErrorCode.EKYC_VERIFICATION_FAILED, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Không thể bóc tách thông tin"));
        verify(fptAiClient, never()).matchFaces(any(), any());
    }

    @Test
    @DisplayName("Lấy trạng thái eKYC: Trả về trạng thái đã định danh và che bớt số CCCD")
    void getEkycStatus_WhenUserVerified_ShouldReturnMaskedIdAndStatus() {
        sampleUser.setIdentityVerified(true);
        sampleUser.setIdCardNumber("079198001234");
        sampleUser.setIdCardName("NGUYỄN VĂN A");
        sampleUser.setEkycConfidenceScore(94.2);
        sampleUser.setEkycVerifiedAt(Instant.now());

        when(userRepository.findByEmail(sampleUser.getEmail())).thenReturn(Optional.of(sampleUser));

        EkycStatusResponse status = ekycService.getEkycStatus(sampleUser.getEmail());

        assertNotNull(status);
        assertTrue(status.isIdentityVerified());
        assertEquals("079******234", status.getIdCardNumberMasked());
        assertEquals("NGUYỄN VĂN A", status.getIdCardName());
        assertEquals(94.2, status.getConfidenceScore());
    }

    @Test
    @DisplayName("Lịch sử eKYC: Lấy danh sách các lần xác thực của người dùng")
    void getEkycHistory_ShouldReturnHistoryList() {
        EkycVerification mockRecord = EkycVerification.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .idCardNumber("079198001234")
                .status("SUCCESS")
                .confidenceScore(91.0)
                .build();

        when(userRepository.findByEmail(sampleUser.getEmail())).thenReturn(Optional.of(sampleUser));
        when(ekycVerificationRepository.findByUserOrderByCreatedAtDesc(sampleUser)).thenReturn(List.of(mockRecord));

        List<EkycVerification> history = ekycService.getEkycHistory(sampleUser.getEmail());

        assertEquals(1, history.size());
        assertEquals("SUCCESS", history.get(0).getStatus());
    }

    @Test
    @DisplayName("Xác thực eKYC thất bại: Số CCCD khai báo không trùng khớp với số trên thẻ CCCD")
    void verifyIdentity_WhenDeclaredCccdDoesNotMatch_ShouldThrowException() {
        FptAiOcrResult mockOcr = FptAiOcrResult.builder()
                .idCardNumber("079198001234")
                .fullName("NGUYỄN VĂN A")
                .dob("15/08/1998")
                .address("Phường 25, Bình Thạnh, TP.HCM")
                .cardType("chip_front")
                .rawJson("{}")
                .build();

        when(userRepository.findByEmail(sampleUser.getEmail())).thenReturn(Optional.of(sampleUser));
        when(fptAiClient.extractIdCard(frontFile, backFile)).thenReturn(mockOcr);
        when(ekycVerificationRepository.save(any(EkycVerification.class))).thenAnswer(i -> i.getArgument(0));

        AppException ex = assertThrows(AppException.class, () ->
                ekycService.verifyIdentity(
                        frontFile, backFile, selfieFile,
                        "012345678901", "NGUYỄN VĂN A", "1998-08-15",
                        null, false, sampleUser.getEmail()
                ));

        assertEquals(ErrorCode.EKYC_VERIFICATION_FAILED, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Số CCCD bạn đã khai báo"));
        verify(fptAiClient, never()).matchFaces(any(), any());
    }

    @Test
    @DisplayName("Xác thực eKYC thất bại: Họ tên khai báo không trùng khớp với tên trên thẻ CCCD")
    void verifyIdentity_WhenDeclaredNameDoesNotMatch_ShouldThrowException() {
        FptAiOcrResult mockOcr = FptAiOcrResult.builder()
                .idCardNumber("079198001234")
                .fullName("NGUYỄN VĂN A")
                .dob("15/08/1998")
                .address("Phường 25, Bình Thạnh, TP.HCM")
                .cardType("chip_front")
                .rawJson("{}")
                .build();

        when(userRepository.findByEmail(sampleUser.getEmail())).thenReturn(Optional.of(sampleUser));
        when(fptAiClient.extractIdCard(frontFile, backFile)).thenReturn(mockOcr);
        when(ekycVerificationRepository.save(any(EkycVerification.class))).thenAnswer(i -> i.getArgument(0));

        AppException ex = assertThrows(AppException.class, () ->
                ekycService.verifyIdentity(
                        frontFile, backFile, selfieFile,
                        "079198001234", "TRẦN VĂN B", "1998-08-15",
                        null, false, sampleUser.getEmail()
                ));

        assertEquals(ErrorCode.EKYC_VERIFICATION_FAILED, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Họ tên bạn đã khai báo"));
        verify(fptAiClient, never()).matchFaces(any(), any());
    }
}
