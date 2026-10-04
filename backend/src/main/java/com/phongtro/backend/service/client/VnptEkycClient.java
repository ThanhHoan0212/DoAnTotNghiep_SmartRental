package com.phongtro.backend.service.client;

import com.phongtro.backend.dto.ekyc.VnptFaceMatchResult;
import com.phongtro.backend.dto.ekyc.VnptOcrResult;
import org.springframework.web.multipart.MultipartFile;

public interface VnptEkycClient {

    /**
     * Bóc tách thông tin thẻ CCCD từ ảnh mặt trước và mặt sau qua dịch vụ VNPT eKYC OCR
     */
    default VnptOcrResult extractIdCard(MultipartFile frontImage, MultipartFile backImage) {
        return extractIdCard(frontImage, backImage, null);
    }

    VnptOcrResult extractIdCard(MultipartFile frontImage, MultipartFile backImage, String customToken);

    /**
     * So khớp khuôn mặt thẻ CCCD với ảnh Selfie qua dịch vụ VNPT eKYC Face Compare
     */
    default VnptFaceMatchResult matchFaces(MultipartFile cardImage, MultipartFile selfieImage) {
        return matchFaces(cardImage, selfieImage, null, false);
    }

    VnptFaceMatchResult matchFaces(MultipartFile cardImage, MultipartFile selfieImage, String customToken, boolean forceMismatch);
}
