package com.phongtro.backend.dto.response;

import com.phongtro.backend.entity.ContractStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thông tin chi tiết hợp đồng thuê phòng")
public class ContractResponse {

    @Schema(description = "ID hợp đồng")
    private UUID id;

    @Schema(description = "Mã số hợp đồng", example = "HD-202609-001")
    private String contractCode;

    // Room info
    @Schema(description = "ID phòng trọ")
    private UUID roomId;

    @Schema(description = "Tiêu đề phòng trọ", example = "Phòng trọ cao cấp Bình Thạnh")
    private String roomTitle;

    @Schema(description = "Địa chỉ phòng", example = "123 Điện Biên Phủ")
    private String roomAddress;

    @Schema(description = "Quận / Huyện", example = "Bình Thạnh")
    private String roomDistrict;

    @Schema(description = "Ảnh đại diện phòng trọ")
    private String primaryImageUrl;

    // Tenant info
    @Schema(description = "ID người thuê")
    private UUID tenantId;

    @Schema(description = "Họ tên người thuê")
    private String tenantName;

    @Schema(description = "Email người thuê")
    private String tenantEmail;

    @Schema(description = "Số điện thoại người thuê")
    private String tenantPhone;

    // Landlord info
    @Schema(description = "ID chủ nhà")
    private UUID landlordId;

    @Schema(description = "Họ tên chủ nhà")
    private String landlordName;

    @Schema(description = "Email chủ nhà")
    private String landlordEmail;

    @Schema(description = "Số điện thoại chủ nhà")
    private String landlordPhone;

    // Terms and prices
    @Schema(description = "Ngày bắt đầu thuê", example = "2026-10-01")
    private LocalDate startDate;

    @Schema(description = "Ngày kết thúc thuê", example = "2027-04-01")
    private LocalDate endDate;

    @Schema(description = "Tiền thuê hàng tháng (VNĐ)", example = "3500000")
    private Double monthlyRent;

    @Schema(description = "Tiền đặt cọc (VNĐ)", example = "3500000")
    private Double depositAmount;

    @Schema(description = "Trạng thái hợp đồng: PENDING, ACTIVE, REJECTED, EXPIRED, TERMINATED, CANCELLED")
    private ContractStatus status;

    @Schema(description = "Điều khoản / Thỏa thuận / Lời nhắn")
    private String terms;

    @Schema(description = "Lý do hủy hoặc từ chối")
    private String cancellationReason;

    @Schema(description = "Thời điểm tạo hợp đồng")
    private Instant createdAt;

    @Schema(description = "Thời điểm cập nhật gần nhất")
    private Instant updatedAt;
}
