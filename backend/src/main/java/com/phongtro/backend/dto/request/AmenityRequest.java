package com.phongtro.backend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu tạo hoặc cập nhật tiện ích phòng trọ")
public class AmenityRequest {

    @NotBlank(message = "Tên tiện ích không được để trống")
    @Size(max = 100, message = "Tên tiện ích không được vượt quá 100 ký tự")
    @Schema(description = "Tên tiện ích", example = "Máy sấy quần áo")
    private String name;

    @Size(max = 50, message = "Tên icon không được vượt quá 50 ký tự")
    @Schema(description = "Tên icon đại diện", example = "wind")
    private String icon;
}
