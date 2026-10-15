package com.phongtro.backend.dto.smartca;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Xác nhận ký số qua VNPT SmartCA")
public class SmartCaConfirmRequest {

    @NotBlank(message = "Mã giao dịch ký số không được để trống")
    @Schema(description = "Mã giao dịch SmartCA", example = "SP_CA_12345678")
    private String transactionId;

    @Schema(description = "Mã PIN/Mật khẩu SmartCA (tùy chọn nếu xác nhận trực tiếp bằng PIN)")
    private String pin;

    @Schema(description = "Mã OTP (nếu SmartCA yêu cầu OTP)")
    private String otp;

    @Schema(description = "Xác nhận ở chế độ Sandbox / Ký mô phỏng nếu người dùng chưa kích hoạt SmartCA thực tế")
    private boolean sandboxConfirm;
}
