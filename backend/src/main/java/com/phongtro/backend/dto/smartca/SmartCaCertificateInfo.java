package com.phongtro.backend.dto.smartca;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thông tin chứng thư số VNPT SmartCA")
public class SmartCaCertificateInfo {

    @Schema(description = "Số serial chứng thư số")
    private String serialNumber;

    @Schema(description = "Chủ thể chứng thư (Subject DN)")
    private String subject;

    @Schema(description = "Tổ chức chứng thực (Issuer)")
    private String issuer;

    @Schema(description = "Hiệu lực từ")
    private String validFrom;

    @Schema(description = "Hiệu lực đến")
    private String validTo;

    @Schema(description = "Trạng thái chứng thư (OPERATIONAL, REVOKED, EXPIRED)")
    private String status;
}
