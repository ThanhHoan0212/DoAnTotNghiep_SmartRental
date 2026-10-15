package com.phongtro.backend.dto.smartca;

import com.phongtro.backend.dto.response.ContractResponse;
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
@Schema(description = "Trạng thái giao dịch ký số VNPT SmartCA")
public class SmartCaStatusResponse {

    @Schema(description = "ID hợp đồng")
    private UUID contractId;

    @Schema(description = "Mã giao dịch")
    private String transactionId;

    @Schema(description = "Trạng thái ký: PENDING, SIGNED, REJECTED, FAILED")
    private String status;

    @Schema(description = "Số serial chứng thư số")
    private String certSerial;

    @Schema(description = "Chủ thể chứng thư số")
    private String certSubject;

    @Schema(description = "Thời điểm ký")
    private String signedAt;

    @Schema(description = "Giá trị chữ ký điện tử")
    private String signatureValue;

    @Schema(description = "Cả hai bên đã hoàn tất ký số và hợp đồng đã kích hoạt ACTIVE")
    private boolean contractCompleted;

    @Schema(description = "Thông báo kết quả")
    private String message;

    @Schema(description = "Thông tin hợp đồng sau cập nhật")
    private ContractResponse contract;
}
