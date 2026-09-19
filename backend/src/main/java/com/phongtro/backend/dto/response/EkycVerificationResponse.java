package com.phongtro.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Kết quả xác thực danh tính điện tử eKYC")
public class EkycVerificationResponse {

    @JsonProperty("isSuccess")
    @Schema(description = "Trạng thái xác thực thành công (điểm > 85%)", example = "true")
    private boolean isSuccess;

    @Schema(description = "Điểm tin cậy đối chiếu khuôn mặt (%)", example = "93.5")
    private Double confidenceScore;

    @Schema(description = "Trạng thái eKYC: SUCCESS hoặc FAILED", example = "SUCCESS")
    private String status;

    @Schema(description = "Thông báo kết quả")
    private String message;

    @Schema(description = "Số CCCD (đã che bớt để bảo mật)", example = "079******123")
    private String idCardNumberMasked;

    @Schema(description = "Họ và tên trên CCCD", example = "NGUYEN VAN A")
    private String idCardName;

    @Schema(description = "Ngày sinh", example = "01/01/1998")
    private String idCardDob;

    @Schema(description = "Nơi thường trú", example = "Quận Bình Thạnh, TP. Hồ Chí Minh")
    private String idCardAddress;

    @Schema(description = "Quê quán", example = "Hà Nội")
    private String idCardHometown;

    @Schema(description = "Thời điểm xác thực")
    private Instant verifiedAt;

    public boolean isSuccess() {
        return isSuccess;
    }

    public boolean getIsSuccess() {
        return isSuccess;
    }
}
