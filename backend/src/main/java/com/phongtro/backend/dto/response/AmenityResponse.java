package com.phongtro.backend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thông tin tiện ích phòng trọ")
public class AmenityResponse {

    @Schema(description = "ID tiện ích", example = "1")
    private Long id;

    @Schema(description = "Tên tiện ích", example = "Wifi tốc độ cao")
    private String name;

    @Schema(description = "Tên icon đại diện", example = "wifi")
    private String icon;
}
