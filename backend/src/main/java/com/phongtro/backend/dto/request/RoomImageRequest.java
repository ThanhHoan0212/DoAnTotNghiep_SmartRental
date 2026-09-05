package com.phongtro.backend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thông tin hình ảnh phòng trọ")
public class RoomImageRequest {

    @NotBlank(message = "Đường dẫn hình ảnh không được để trống")
    @Schema(description = "URL hình ảnh", example = "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267")
    private String imageUrl;

    @Schema(description = "Ảnh đại diện chính", example = "true", defaultValue = "false")
    @Builder.Default
    private boolean isPrimary = false;
}
