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
@Schema(description = "Trạng thái định danh eKYC của người dùng")
public class EkycStatusResponse {

    @JsonProperty("isIdentityVerified")
    @Schema(description = "Đã xác thực danh tính thật hay chưa", example = "true")
    private boolean isIdentityVerified;

    @Schema(description = "Số CCCD (đã che để bảo mật)", example = "079******123")
    private String idCardNumberMasked;

    @Schema(description = "Họ và tên trên CCCD", example = "NGUYEN VAN A")
    private String idCardName;

    @Schema(description = "Điểm tin cậy đối chiếu khuôn mặt (%)", example = "92.5")
    private Double confidenceScore;

    @Schema(description = "Thời điểm xác thực thành công")
    private Instant verifiedAt;

    public boolean isIdentityVerified() {
        return isIdentityVerified;
    }

    public boolean getIsIdentityVerified() {
        return isIdentityVerified;
    }
}
