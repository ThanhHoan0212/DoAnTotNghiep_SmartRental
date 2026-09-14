package com.phongtro.backend.dto.response;

import com.phongtro.backend.common.PageResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Kết quả tìm kiếm AI thông minh kèm thông tin phân tích query")
public class NlpSearchResponse {

    @Schema(description = "Chi tiết các bộ lọc đã được AI phân tích từ câu người dùng")
    private ParsedQueryResponse parsedQuery;

    @Schema(description = "Danh sách phòng trọ tìm được phù hợp với yêu cầu")
    private PageResponse<RoomSummaryResponse> results;
}
