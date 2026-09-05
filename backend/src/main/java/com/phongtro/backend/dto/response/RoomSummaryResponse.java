package com.phongtro.backend.dto.response;

import com.phongtro.backend.entity.RoomStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thông tin tóm tắt phòng trọ (phục vụ hiển thị danh sách dạng card)")
public class RoomSummaryResponse {

    @Schema(description = "ID phòng trọ")
    private UUID id;

    @Schema(description = "Tiêu đề tin đăng")
    private String title;

    @Schema(description = "Giá thuê hàng tháng (VNĐ)")
    private Double price;

    @Schema(description = "Diện tích phòng (m2)")
    private Double area;

    @Schema(description = "Địa chỉ số nhà, tên đường")
    private String address;

    @Schema(description = "Quận / Huyện")
    private String district;

    @Schema(description = "Tỉnh / Thành phố")
    private String city;

    @Schema(description = "URL ảnh đại diện chính của phòng")
    private String primaryImageUrl;

    @Schema(description = "Trạng thái tin đăng")
    private RoomStatus status;

    @Schema(description = "Số lượt xem")
    private Long viewCount;

    @Schema(description = "Thời gian đăng tin")
    private Instant createdAt;
}
