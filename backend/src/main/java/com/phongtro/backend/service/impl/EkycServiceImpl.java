package com.phongtro.backend.service.impl;

import com.phongtro.backend.config.FptAiProperties;
import com.phongtro.backend.dto.ekyc.FptAiFaceMatchResult;
import com.phongtro.backend.dto.ekyc.FptAiOcrResult;
import com.phongtro.backend.dto.response.EkycStatusResponse;
import com.phongtro.backend.dto.response.EkycVerificationResponse;
import com.phongtro.backend.entity.EkycVerification;
import com.phongtro.backend.entity.User;
import com.phongtro.backend.exception.AppException;
import com.phongtro.backend.exception.ErrorCode;
import com.phongtro.backend.repository.EkycVerificationRepository;
import com.phongtro.backend.repository.UserRepository;
import com.phongtro.backend.service.EkycService;
import com.phongtro.backend.service.client.FptAiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EkycServiceImpl implements EkycService {

    private final UserRepository userRepository;
    private final EkycVerificationRepository ekycVerificationRepository;
    private final FptAiClient fptAiClient;
    private final FptAiProperties fptAiProperties;

    @Override
    @Transactional
    public EkycVerificationResponse verifyIdentity(
            MultipartFile frontImage,
            MultipartFile backImage,
            MultipartFile selfieImage,
            String declaredIdCardNumber,
            String declaredFullName,
            String declaredDob,
            String customApiKey,
            boolean simulatedMismatch,
            String userEmail
    ) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // 1. Kiểm tra tính hợp lệ của các file ảnh tải lên
        validateImageFile(frontImage, "Ảnh mặt trước CCCD");
        validateImageFile(backImage, "Ảnh mặt sau CCCD");
        validateImageFile(selfieImage, "Ảnh chân dung selfie");

        log.info("Processing eKYC for user [{}] with FPT.AI (Front size: {}, Back size: {}, Selfie size: {})",
                userEmail, frontImage.getSize(), backImage.getSize(), selfieImage.getSize());

        // 2. Bước 1: Gọi FPT.AI trích xuất thông tin thẻ Căn cước công dân (OCR)
        FptAiOcrResult ocrResult = (customApiKey != null && !customApiKey.trim().isEmpty())
                ? fptAiClient.extractIdCard(frontImage, backImage, customApiKey)
                : fptAiClient.extractIdCard(frontImage, backImage);
        if (ocrResult == null || ocrResult.getIdCardNumber() == null || ocrResult.getIdCardNumber().trim().isEmpty()) {
            throw new AppException(ErrorCode.EKYC_VERIFICATION_FAILED,
                    "Không thể bóc tách thông tin thẻ CCCD hợp lệ từ ảnh tải lên. Vui lòng chụp rõ nét, đủ ánh sáng và không bị lóa.");
        }

        // 2.1. ĐỐI CHIẾU THÔNG TIN KHAI BÁO BƯỚC 1 VỚI KẾT QUẢ BÓC TÁCH TỪ THẺ CCCD
        // A. Kiểm tra số thẻ CCCD:
        if (declaredIdCardNumber != null && !declaredIdCardNumber.trim().isEmpty()) {
            String cleanDeclared = declaredIdCardNumber.trim().replaceAll("[^0-9]", "");
            String cleanOcr = ocrResult.getIdCardNumber() != null ? ocrResult.getIdCardNumber().trim().replaceAll("[^0-9]", "") : "";

            if (!cleanDeclared.equals(cleanOcr)) {
                String failureReason = String.format(
                        "Thông tin không trùng khớp: Số CCCD bạn đã khai báo [%s] khác với số CCCD nhận diện trên thẻ [%s]. Vui lòng kiểm tra lại!",
                        declaredIdCardNumber, cleanOcr
                );
                log.warn("eKYC cross-check failed for user [{}]: Declared CCCD [{}] != OCR CCCD [{}]", userEmail, declaredIdCardNumber, cleanOcr);

                // Lưu nhật ký kiểm tra thất bại vào database
                EkycVerification verification = EkycVerification.builder()
                        .user(user)
                        .idCardNumber(ocrResult.getIdCardNumber())
                        .idCardName(ocrResult.getFullName())
                        .idCardDob(ocrResult.getDob())
                        .idCardAddress(ocrResult.getAddress())
                        .idCardHometown(ocrResult.getHometown())
                        .confidenceScore(0.0)
                        .status("FAILED")
                        .failureReason(failureReason)
                        .rawOcrResponse(ocrResult.getRawJson())
                        .build();
                ekycVerificationRepository.save(verification);

                throw new AppException(ErrorCode.EKYC_VERIFICATION_FAILED, failureReason);
            }
        }

        // B. Kiểm tra Họ và tên:
        if (declaredFullName != null && !declaredFullName.trim().isEmpty()) {
            String cleanDeclaredName = declaredFullName.trim().toUpperCase();
            String cleanOcrName = ocrResult.getFullName() != null ? ocrResult.getFullName().trim().toUpperCase() : "";

            if (!cleanDeclaredName.equals(cleanOcrName)) {
                String failureReason = String.format(
                        "Thông tin không trùng khớp: Họ tên bạn đã khai báo [%s] khác với họ tên nhận diện trên thẻ CCCD [%s].",
                        declaredFullName, cleanOcrName
                );
                log.warn("eKYC cross-check failed for user [{}]: Declared Name [{}] != OCR Name [{}]", userEmail, declaredFullName, cleanOcrName);

                EkycVerification verification = EkycVerification.builder()
                        .user(user)
                        .idCardNumber(ocrResult.getIdCardNumber())
                        .idCardName(ocrResult.getFullName())
                        .idCardDob(ocrResult.getDob())
                        .idCardAddress(ocrResult.getAddress())
                        .idCardHometown(ocrResult.getHometown())
                        .confidenceScore(0.0)
                        .status("FAILED")
                        .failureReason(failureReason)
                        .rawOcrResponse(ocrResult.getRawJson())
                        .build();
                ekycVerificationRepository.save(verification);

                throw new AppException(ErrorCode.EKYC_VERIFICATION_FAILED, failureReason);
            }
        }

        // 3. Bước 2: Gọi FPT.AI so khớp khuôn mặt trên thẻ CCCD với ảnh chân dung Selfie (Face Match)
        FptAiFaceMatchResult faceMatchResult = (customApiKey != null || simulatedMismatch)
                ? fptAiClient.matchFaces(frontImage, selfieImage, customApiKey, simulatedMismatch)
                : fptAiClient.matchFaces(frontImage, selfieImage);
        double similarity = (faceMatchResult != null && faceMatchResult.getSimilarity() != null)
                ? faceMatchResult.getSimilarity()
                : 0.0;

        double requiredThreshold = (fptAiProperties.getConfidenceThreshold() != null)
                ? fptAiProperties.getConfidenceThreshold()
                : 85.0;

        boolean isPassed = similarity > requiredThreshold;
        String status = isPassed ? "SUCCESS" : "FAILED";
        String failureReason = isPassed ? null : String.format(
                "Độ tương đồng khuôn mặt (%.1f%%) chưa đạt ngưỡng tin cậy an toàn yêu cầu (> %.1f%%). Khuôn mặt trên thẻ CCCD và ảnh chân dung Selfie không phải cùng một người.",
                similarity, requiredThreshold
        );

        // 4. Lưu lịch sử lượt xác thực eKYC vào cơ sở dữ liệu (Audit History)
        EkycVerification verification = EkycVerification.builder()
                .user(user)
                .idCardNumber(ocrResult.getIdCardNumber())
                .idCardName(ocrResult.getFullName())
                .idCardDob(ocrResult.getDob())
                .idCardAddress(ocrResult.getAddress())
                .idCardHometown(ocrResult.getHometown())
                .confidenceScore(similarity)
                .status(status)
                .failureReason(failureReason)
                .rawOcrResponse(ocrResult.getRawJson())
                .rawFaceResponse(faceMatchResult != null ? faceMatchResult.getRawJson() : null)
                .build();

        ekycVerificationRepository.save(verification);

        // 5. Đánh giá kết quả: Nếu không đạt ngưỡng > 85%, ném lỗi từ chối định danh
        if (!isPassed) {
            log.warn("eKYC failed for user [{}]: Confidence score {}% is below required {}%",
                    userEmail, similarity, requiredThreshold);
            throw new AppException(ErrorCode.EKYC_VERIFICATION_FAILED, failureReason);
        }

        // 6. Cập nhật thông tin định danh cho tài khoản người dùng
        Instant now = Instant.now();
        user.setIdentityVerified(true);
        user.setIdCardNumber(ocrResult.getIdCardNumber());
        user.setIdCardName(ocrResult.getFullName());
        user.setIdCardDob(ocrResult.getDob());
        user.setIdCardAddress(ocrResult.getAddress());
        user.setIdCardHometown(ocrResult.getHometown());
        user.setIdCardIssueDate(ocrResult.getIssueDate());
        user.setEkycConfidenceScore(similarity);
        user.setEkycVerifiedAt(now);

        userRepository.save(user);

        log.info("eKYC successfully verified for user [{}] with confidence score: {}%", userEmail, similarity);

        return EkycVerificationResponse.builder()
                .isSuccess(true)
                .confidenceScore(similarity)
                .status("SUCCESS")
                .message("Xác thực danh tính điện tử eKYC thành công qua FPT.AI")
                .idCardNumberMasked(maskIdCardNumber(ocrResult.getIdCardNumber()))
                .idCardName(ocrResult.getFullName())
                .idCardDob(ocrResult.getDob())
                .idCardAddress(ocrResult.getAddress())
                .idCardHometown(ocrResult.getHometown())
                .verifiedAt(now)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public EkycStatusResponse getEkycStatus(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        return EkycStatusResponse.builder()
                .isIdentityVerified(user.isIdentityVerified())
                .idCardNumberMasked(maskIdCardNumber(user.getIdCardNumber()))
                .idCardName(user.getIdCardName())
                .confidenceScore(user.getEkycConfidenceScore())
                .verifiedAt(user.getEkycVerifiedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EkycVerification> getEkycHistory(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        return ekycVerificationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    private void validateImageFile(MultipartFile file, String fieldName) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, fieldName + " không được để trống.");
        }
        // Giới hạn 10MB
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, fieldName + " vượt quá dung lượng tối đa 10MB.");
        }
    }

    private String maskIdCardNumber(String idCardNumber) {
        if (idCardNumber == null || idCardNumber.length() < 6) {
            return idCardNumber;
        }
        int len = idCardNumber.length();
        return idCardNumber.substring(0, 3) + "******" + idCardNumber.substring(len - 3);
    }
}
