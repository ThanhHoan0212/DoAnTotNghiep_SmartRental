package com.phongtro.backend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thông tin hình ảnh phòng trọ")
public class RoomImageResponse {

    @Schema(description = "ID hình ảnh", example = "c0000000-0000-0000-0000-000000000001")
    private UUID id;

    @Schema(description = "URL hình ảnh", example = "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267")
    private String imageUrl;

    @Schema(description = "Ảnh đại diện chính của phòng", example = "true")
    private boolean isPrimary;
}
