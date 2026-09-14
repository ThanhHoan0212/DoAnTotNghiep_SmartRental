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
@Schema(description = "Số lượng tin đăng phòng trọ theo khu vực quận / huyện")
public class DistrictCountResponse {

    @Schema(description = "Tên quận / huyện", example = "Bình Thạnh")
    private String district;

    @Schema(description = "Số lượng phòng trọ đang có sẵn", example = "15")
    private Long count;
}
