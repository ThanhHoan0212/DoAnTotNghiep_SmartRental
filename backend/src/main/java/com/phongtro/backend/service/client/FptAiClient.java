package com.phongtro.backend.service.client;

import com.phongtro.backend.dto.ekyc.FptAiFaceMatchResult;
import com.phongtro.backend.dto.ekyc.FptAiOcrResult;
import org.springframework.web.multipart.MultipartFile;

public interface FptAiClient {

    /**
     * Bóc tách thông tin thẻ CCCD từ ảnh mặt trước và mặt sau
     */
    default FptAiOcrResult extractIdCard(MultipartFile frontImage, MultipartFile backImage) {
        return extractIdCard(frontImage, backImage, null);
    }

    FptAiOcrResult extractIdCard(MultipartFile frontImage, MultipartFile backImage, String customApiKey);

    /**
     * Đối chiếu khuôn mặt trên thẻ CCCD với ảnh chân dung Selfie
     */
    default FptAiFaceMatchResult matchFaces(MultipartFile cardImage, MultipartFile selfieImage) {
        return matchFaces(cardImage, selfieImage, null, false);
    }

    FptAiFaceMatchResult matchFaces(MultipartFile cardImage, MultipartFile selfieImage, String customApiKey, boolean forceMismatch);
}
