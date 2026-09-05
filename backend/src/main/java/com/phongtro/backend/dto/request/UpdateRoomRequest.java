package com.phongtro.backend.dto.request;

import com.phongtro.backend.entity.RoomStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
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
@Schema(description = "Yêu cầu cập nhật thông tin phòng trọ")
public class UpdateRoomRequest {

    @Size(max = 255, message = "Tiêu đề không được vượt quá 255 ký tự")
    @Schema(description = "Tiêu đề tin đăng", example = "Phòng trọ cao cấp full nội thất gần ĐH Quốc Gia Cầu Giấy")
    private String title;

    @Schema(description = "Mô tả chi tiết phòng trọ")
    private String description;

    @DecimalMin(value = "0.0", inclusive = false, message = "Giá thuê phải lớn hơn 0")
    @Schema(description = "Giá thuê hàng tháng (VNĐ)", example = "3800000")
    private Double price;

    @DecimalMin(value = "1.0", message = "Diện tích tối thiểu phải từ 1m2 trở lên")
    @Schema(description = "Diện tích phòng (m2)", example = "28.0")
    private Double area;

    @Size(max = 255, message = "Địa chỉ không được vượt quá 255 ký tự")
    @Schema(description = "Địa chỉ số nhà, tên đường")
    private String address;

    @Schema(description = "Phường / Xã")
    private String ward;

    @Schema(description = "Quận / Huyện")
    private String district;

    @Schema(description = "Tỉnh / Thành phố")
    private String city;

    @Schema(description = "Tọa độ vĩ độ (Latitude)")
    private Double latitude;

    @Schema(description = "Tọa độ kinh độ (Longitude)")
    private Double longitude;

    @Schema(description = "Trạng thái tin đăng (Chủ nhà có thể ẩn hoặc mở lại tin)")
    private RoomStatus status;

    @Schema(description = "Danh sách ID tiện ích mới")
    private List<Long> amenityIds;

    @Schema(description = "Danh sách hình ảnh mới")
    private List<RoomImageRequest> images;
}
