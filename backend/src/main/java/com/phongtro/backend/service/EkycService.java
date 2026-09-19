package com.phongtro.backend.service;

import com.phongtro.backend.dto.response.EkycStatusResponse;
import com.phongtro.backend.dto.response.EkycVerificationResponse;
import com.phongtro.backend.entity.EkycVerification;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface EkycService {

    /**
     * Xác thực danh tính điện tử eKYC với FPT.AI (CCCD OCR & Face Match) kèm đối chiếu thông tin khai báo
     */
    default EkycVerificationResponse verifyIdentity(
            MultipartFile frontImage,
            MultipartFile backImage,
            MultipartFile selfieImage,
            String userEmail
    ) {
        return verifyIdentity(frontImage, backImage, selfieImage, null, null, null, null, false, userEmail);
    }

    EkycVerificationResponse verifyIdentity(
            MultipartFile frontImage,
            MultipartFile backImage,
            MultipartFile selfieImage,
            String declaredIdCardNumber,
            String declaredFullName,
            String declaredDob,
            String customApiKey,
            boolean simulatedMismatch,
            String userEmail
    );

    /**
     * Lấy trạng thái định danh eKYC hiện tại của người dùng
     */
    EkycStatusResponse getEkycStatus(String userEmail);

    /**
     * Lịch sử các lần xác thực eKYC của người dùng
     */
    List<EkycVerification> getEkycHistory(String userEmail);
}
