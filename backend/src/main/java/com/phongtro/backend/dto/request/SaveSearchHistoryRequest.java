package com.phongtro.backend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu lưu lịch sử tìm kiếm")
public class SaveSearchHistoryRequest {

    @Schema(description = "Câu truy vấn tìm kiếm hoặc từ khóa", example = "tìm phòng trọ ở Bình Thạnh dưới 4 triệu")
    private String queryText;

    @Schema(description = "Quận / Huyện đã lọc", example = "Bình Thạnh")
    private String district;

    @Schema(description = "Mức giá tối thiểu", example = "2000000")
    private Double minPrice;

    @Schema(description = "Mức giá tối đa", example = "4000000")
    private Double maxPrice;
}
