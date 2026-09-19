package com.phongtro.backend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu đặt thuê phòng / Tạo hợp đồng")
public class CreateContractRequest {

    @NotNull(message = "ID phòng trọ không được để trống")
    @Schema(description = "ID phòng trọ muốn thuê", example = "b0000000-0000-0000-0000-000000000001")
    private UUID roomId;

    @NotNull(message = "Ngày bắt đầu thuê không được để trống")
    @FutureOrPresent(message = "Ngày bắt đầu thuê phải từ ngày hiện tại trở đi")
    @Schema(description = "Ngày bắt đầu dọn vào ở", example = "2026-10-01")
    private LocalDate startDate;

    @NotNull(message = "Ngày kết thúc thuê không được để trống")
    @Schema(description = "Ngày kết thúc kỳ hạn thuê", example = "2027-04-01")
    private LocalDate endDate;

    @Schema(description = "Số tiền đặt cọc dự kiến (nếu để trống hệ thống tự tính bằng 1 tháng tiền phòng)", example = "3500000")
    private Double depositAmount;

    @Schema(description = "Điều khoản bổ sung hoặc lời nhắn gửi chủ nhà", example = "Em muốn chuyển vào đầu tháng sau, xin được gửi cọc giữ phòng trước.")
    private String terms;
}
