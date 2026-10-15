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
@Schema(description = "Yêu cầu khởi tạo ký số hợp đồng qua VNPT SmartCA")
public class SmartCaInitiateRequest {

    @Schema(description = "Số CCCD/CMND người ký (nếu để trống sẽ lấy từ hồ sơ eKYC của tài khoản)", example = "0131930813216")
    private String idCardNumber;

    @Schema(description = "Số chứng thư số (nếu có nhiều chứng thư, để trống để chọn chứng thư mặc định)", example = "")
    private String serialNumber;
}
