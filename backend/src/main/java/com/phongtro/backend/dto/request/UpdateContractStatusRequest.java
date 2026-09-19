package com.phongtro.backend.dto.request;

import com.phongtro.backend.entity.ContractStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu cập nhật trạng thái hợp đồng (Duyệt, từ chối, hủy, thanh lý)")
public class UpdateContractStatusRequest {

    @NotNull(message = "Trạng thái mới không được để trống")
    @Schema(description = "Trạng thái mới: ACTIVE (Chấp thuận), REJECTED (Từ chối), CANCELLED (Hủy yêu cầu), TERMINATED (Thanh lý)", example = "ACTIVE")
    private ContractStatus status;

    @Schema(description = "Lý do từ chối, hủy hoặc thanh lý", example = "Chủ nhà đồng ý cho thuê từ đầu tháng 10")
    private String reason;
}
