package com.phongtro.backend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thông tin một mục lịch sử tìm kiếm gần đây")
public class SearchHistoryResponse {

    @Schema(description = "ID bản ghi lịch sử", example = "c0000000-0000-0000-0000-000000000001")
    private UUID id;

    @Schema(description = "Câu truy vấn tìm kiếm hoặc từ khóa", example = "tìm phòng trọ ở Bình Thạnh dưới 4 triệu")
    private String queryText;

    @Schema(description = "Quận / Huyện đã tìm kiếm", example = "Bình Thạnh")
    private String district;

    @Schema(description = "Mức giá tối thiểu", example = "2000000")
    private Double minPrice;

    @Schema(description = "Mức giá tối đa", example = "4000000")
    private Double maxPrice;

    @Schema(description = "Thời điểm tìm kiếm")
    private Instant searchedAt;
}
