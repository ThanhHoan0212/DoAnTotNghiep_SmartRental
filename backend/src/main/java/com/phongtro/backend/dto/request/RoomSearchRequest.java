package com.phongtro.backend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Tham số tìm kiếm phòng trọ nâng cao đa tiêu chí")
public class RoomSearchRequest {

    @Schema(description = "Từ khóa tìm kiếm (tiêu đề, mô tả, địa chỉ, đường)", example = "khép kín")
    private String keyword;

    @Schema(description = "Quận / Huyện", example = "Bình Thạnh")
    private String district;

    @Schema(description = "Phường / Xã", example = "Phường 15")
    private String ward;

    @Schema(description = "Tỉnh / Thành phố", example = "Hồ Chí Minh", defaultValue = "Hồ Chí Minh")
    @Builder.Default
    private String city = "Hồ Chí Minh";

    @Schema(description = "Mức giá thuê tối thiểu (VNĐ)", example = "2000000")
    private Double minPrice;

    @Schema(description = "Mức giá thuê tối đa (VNĐ)", example = "4500000")
    private Double maxPrice;

    @Schema(description = "Diện tích tối thiểu (m2)", example = "20.0")
    private Double minArea;

    @Schema(description = "Diện tích tối đa (m2)", example = "40.0")
    private Double maxArea;

    @Schema(description = "Danh sách ID các tiện ích bắt buộc phải có (AND logic)", example = "[1, 2, 3]")
    @Builder.Default
    private List<Long> amenityIds = new ArrayList<>();

    @Schema(description = "Tọa độ vĩ độ tâm tìm kiếm (Latitude)", example = "21.0333")
    private Double latitude;

    @Schema(description = "Tọa độ kinh độ tâm tìm kiếm (Longitude)", example = "105.7944")
    private Double longitude;

    @Schema(description = "Bán kính tìm kiếm tính từ tâm tọa độ (km)", example = "3.0")
    private Double radiusKm;

    @Schema(description = "Tiêu chí sắp xếp: 'createdAt', 'price', 'area', 'viewCount'", example = "createdAt", defaultValue = "createdAt")
    @Builder.Default
    private String sortBy = "createdAt";

    @Schema(description = "Chiều sắp xếp: 'asc' (tăng dần) hoặc 'desc' (giảm dần)", example = "desc", defaultValue = "desc")
    @Builder.Default
    private String sortDirection = "desc";

    @Schema(description = "Số trang (bắt đầu từ 0)", example = "0", defaultValue = "0")
    @Builder.Default
    private int page = 0;

    @Schema(description = "Số bản ghi mỗi trang", example = "10", defaultValue = "10")
    @Builder.Default
    private int size = 10;
}
