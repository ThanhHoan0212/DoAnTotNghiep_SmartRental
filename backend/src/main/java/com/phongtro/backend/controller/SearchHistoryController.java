package com.phongtro.backend.controller;

import com.phongtro.backend.common.ApiResponse;
import com.phongtro.backend.dto.request.SaveSearchHistoryRequest;
import com.phongtro.backend.dto.response.SearchHistoryResponse;
import com.phongtro.backend.service.SearchHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/search-history")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "8. Search History Management", description = "Quản lý lịch sử tìm kiếm phòng trọ của người dùng")
public class SearchHistoryController {

    private final SearchHistoryService searchHistoryService;

    @PostMapping
    @Operation(summary = "Lưu vết tìm kiếm phòng trọ",
            description = "Tự động ghi nhận từ khóa, bộ lọc hoặc prompt AI mà người dùng vừa thực hiện")
    public ResponseEntity<ApiResponse<Void>> saveSearchHistory(
            Authentication authentication,
            @Valid @RequestBody SaveSearchHistoryRequest request) {
        searchHistoryService.saveSearchHistory(request, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Lưu lịch sử tìm kiếm thành công", null));
    }

    @GetMapping("/recent")
    @Operation(summary = "Lấy danh sách 10 tìm kiếm gần đây nhất",
            description = "Trả về tối đa 10 lượt tìm kiếm mới nhất của người dùng hiện tại")
    public ResponseEntity<ApiResponse<List<SearchHistoryResponse>>> getRecentSearches(
            Authentication authentication) {
        List<SearchHistoryResponse> response = searchHistoryService.getRecentSearches(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa một mục tìm kiếm theo ID",
            description = "Xóa một bản ghi lịch sử tìm kiếm cụ thể thuộc quyền sở hữu của người dùng hiện tại")
    public ResponseEntity<ApiResponse<Void>> deleteSearchHistoryItem(
            Authentication authentication,
            @PathVariable UUID id) {
        searchHistoryService.deleteSearchHistoryItem(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Đã xóa mục lịch sử tìm kiếm", null));
    }

    @DeleteMapping("/all")
    @Operation(summary = "Xóa toàn bộ lịch sử tìm kiếm",
            description = "Xóa tất cả các bản ghi tìm kiếm của người dùng hiện tại")
    public ResponseEntity<ApiResponse<Void>> clearAllSearchHistory(
            Authentication authentication) {
        searchHistoryService.clearAllSearchHistory(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Đã xóa toàn bộ lịch sử tìm kiếm", null));
    }
}
