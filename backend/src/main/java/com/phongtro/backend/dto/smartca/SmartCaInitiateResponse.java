package com.phongtro.backend.dto.smartca;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Kết quả khởi tạo yêu cầu ký số VNPT SmartCA")
public class SmartCaInitiateResponse {

    @Schema(description = "ID hợp đồng")
    private UUID contractId;

    @Schema(description = "Mã giao dịch ký số SmartCA")
    private String transactionId;

    @Schema(description = "Mã tài liệu")
    private String docId;

    @Schema(description = "Mã băm SHA-256 nội dung hợp đồng gửi ký")
    private String dataHash;

    @Schema(description = "Số CCCD người ký")
    private String signerCccd;

    @Schema(description = "Họ tên người ký")
    private String signerName;

    @Schema(description = "Vai trò người ký: TENANT hoặc LANDLORD")
    private String signerRole;

    @Schema(description = "Trạng thái giao dịch: PENDING_CONFIRMATION, SIGNED, FAILED")
    private String status;

    @Schema(description = "Thông báo hướng dẫn")
    private String message;

    @Schema(description = "Đang chạy chế độ Sandbox / Demo")
    private boolean sandbox;
}
