package com.phongtro.backend.controller;

import com.phongtro.backend.common.ApiResponse;
import com.phongtro.backend.common.PageResponse;
import com.phongtro.backend.dto.request.NlpSearchRequest;
import com.phongtro.backend.dto.request.RoomSearchRequest;
import com.phongtro.backend.dto.response.DistrictCountResponse;
import com.phongtro.backend.dto.response.NlpSearchResponse;
import com.phongtro.backend.dto.response.ParsedQueryResponse;
import com.phongtro.backend.dto.response.RoomSummaryResponse;
import com.phongtro.backend.service.RoomSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
@Tag(name = "6. Smart Search & Filter", description = "Tìm kiếm nâng cao đa tiêu chí & Tìm kiếm thông minh bằng ngôn ngữ tự nhiên (AI / NLP)")
public class RoomSearchController {

    private final RoomSearchService roomSearchService;

    @PostMapping("/search")
    @Operation(summary = "Tìm kiếm phòng trọ nâng cao đa tiêu chí",
            description = "Tìm kiếm kết hợp từ khóa, khoảng giá, diện tích, quận/huyện, nhiều tiện ích (AND logic) và bán kính tọa độ vị trí")
    public ResponseEntity<ApiResponse<PageResponse<RoomSummaryResponse>>> searchRooms(
            @RequestBody(required = false) RoomSearchRequest request) {
        if (request == null) {
            request = new RoomSearchRequest();
        }
        PageResponse<RoomSummaryResponse> response = roomSearchService.searchRooms(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/search/nlp")
    @Operation(summary = "Tìm kiếm thông minh bằng ngôn ngữ tự nhiên (AI NLP Search)",
            description = "Người dùng nhập câu tự nhiên (vd: 'tìm phòng trọ khép kín ở Bình Thạnh dưới 4 triệu có điều hòa'). " +
                    "Hệ thống tự động phân tích câu qua AI Service (hoặc Rule-based Regex fallback) và lọc phòng tương ứng.")
    public ResponseEntity<ApiResponse<NlpSearchResponse>> searchByNaturalLanguage(
            @Valid @RequestBody NlpSearchRequest request) {
        NlpSearchResponse response = roomSearchService.searchByNaturalLanguage(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/search/parse")
    @Operation(summary = "Phân tích câu tìm kiếm tự nhiên (Preview Parse)",
            description = "API kiểm thử/xem trước việc bóc tách thực thể (Entity Extraction) từ câu tìm kiếm tiếng Việt của người dùng")
    public ResponseEntity<ApiResponse<ParsedQueryResponse>> parseQuery(
            @RequestParam String query) {
        ParsedQueryResponse response = roomSearchService.parseNaturalLanguageQuery(query);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/districts-summary")
    @Operation(summary = "Thống kê số lượng phòng trọ theo quận / huyện",
            description = "Lấy danh sách quận/huyện kèm số lượng phòng trọ đang có sẵn để hiển thị widget lọc nhanh")
    public ResponseEntity<ApiResponse<List<DistrictCountResponse>>> getDistrictRoomCounts() {
        List<DistrictCountResponse> response = roomSearchService.getDistrictRoomCounts();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
