package com.phongtro.backend.dto.response;

import com.phongtro.backend.entity.RoomStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thông tin chi tiết đầy đủ của phòng trọ")
public class RoomResponse {

    @Schema(description = "ID phòng trọ", example = "b0000000-0000-0000-0000-000000000001")
    private UUID id;

    @Schema(description = "Thông tin chủ nhà đăng tin")
    private UserResponse landlord;

    @Schema(description = "Tiêu đề tin đăng")
    private String title;

    @Schema(description = "Mô tả chi tiết")
    private String description;

    @Schema(description = "Giá thuê hàng tháng (VNĐ)", example = "3800000")
    private Double price;

    @Schema(description = "Diện tích phòng (m2)", example = "28.0")
    private Double area;

    @Schema(description = "Địa chỉ đầy đủ")
    private String address;

    @Schema(description = "Phường / Xã")
    private String ward;

    @Schema(description = "Quận / Huyện")
    private String district;

    @Schema(description = "Tỉnh / Thành phố")
    private String city;

    @Schema(description = "Tọa độ vĩ độ")
    private Double latitude;

    @Schema(description = "Tọa độ kinh độ")
    private Double longitude;

    @Schema(description = "Trạng thái tin đăng", example = "APPROVED")
    private RoomStatus status;

    @Schema(description = "Số lượt xem tin", example = "142")
    private Long viewCount;

    @Schema(description = "Danh sách hình ảnh phòng")
    @Builder.Default
    private List<RoomImageResponse> images = new ArrayList<>();

    @Schema(description = "Danh sách tiện ích có trong phòng")
    @Builder.Default
    private List<AmenityResponse> amenities = new ArrayList<>();

    @Schema(description = "Thời gian đăng tin")
    private Instant createdAt;

    @Schema(description = "Thời gian cập nhật gần nhất")
    private Instant updatedAt;
}
