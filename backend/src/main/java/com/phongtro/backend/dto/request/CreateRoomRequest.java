package com.phongtro.backend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
@Schema(description = "Yêu cầu đăng tin phòng trọ mới")
public class CreateRoomRequest {

    @NotBlank(message = "Tiêu đề tin đăng không được để trống")
    @Size(max = 255, message = "Tiêu đề không được vượt quá 255 ký tự")
    @Schema(description = "Tiêu đề tin đăng", example = "Phòng trọ cao cấp full nội thất gần ĐH Quốc Gia Cầu Giấy")
    private String title;

    @Schema(description = "Mô tả chi tiết phòng trọ", example = "Phòng mới xây 100%, đầy đủ điều hòa, nóng lạnh, giường tủ...")
    private String description;

    @NotNull(message = "Giá thuê không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá thuê phải lớn hơn 0")
    @Schema(description = "Giá thuê hàng tháng (VNĐ)", example = "3500000")
    private Double price;

    @NotNull(message = "Diện tích không được để trống")
    @DecimalMin(value = "1.0", message = "Diện tích tối thiểu phải từ 1m2 trở lên")
    @Schema(description = "Diện tích phòng (m2)", example = "25.5")
    private Double area;

    @NotBlank(message = "Địa chỉ cụ thể không được để trống")
    @Size(max = 255, message = "Địa chỉ không được vượt quá 255 ký tự")
    @Schema(description = "Địa chỉ số nhà, tên đường", example = "Số 18 Ngõ 175 Cầu Giấy")
    private String address;

    @Schema(description = "Phường / Xã", example = "Dịch Vọng")
    private String ward;

    @NotBlank(message = "Quận / Huyện không được để trống")
    @Schema(description = "Quận / Huyện", example = "Cầu Giấy")
    private String district;

    @Schema(description = "Tỉnh / Thành phố", example = "Hà Nội", defaultValue = "Hà Nội")
    @Builder.Default
    private String city = "Hà Nội";

    @Schema(description = "Tọa độ vĩ độ (Latitude)", example = "21.0333")
    private Double latitude;

    @Schema(description = "Tọa độ kinh độ (Longitude)", example = "105.7944")
    private Double longitude;

    @Schema(description = "Danh sách ID các tiện ích có trong phòng", example = "[1, 2, 3, 4]")
    @Builder.Default
    private List<Long> amenityIds = new ArrayList<>();

    @Schema(description = "Danh sách URL hình ảnh phòng trọ")
    @Builder.Default
    private List<RoomImageRequest> images = new ArrayList<>();
}
