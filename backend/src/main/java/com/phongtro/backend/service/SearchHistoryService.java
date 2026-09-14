package com.phongtro.backend.service;

import com.phongtro.backend.dto.request.SaveSearchHistoryRequest;
import com.phongtro.backend.dto.response.SearchHistoryResponse;

import java.util.List;
import java.util.UUID;

public interface SearchHistoryService {

    /**
     * Tự động lưu vết tìm kiếm của người dùng
     */
    void saveSearchHistory(SaveSearchHistoryRequest request, String userEmail);

    /**
     * Lấy danh sách Top 10 tìm kiếm gần đây nhất của người dùng
     */
    List<SearchHistoryResponse> getRecentSearches(String userEmail);

    /**
     * Xóa 1 bản ghi lịch sử tìm kiếm theo ID
     */
    void deleteSearchHistoryItem(UUID id, String userEmail);

    /**
     * Xóa toàn bộ lịch sử tìm kiếm của người dùng
     */
    void clearAllSearchHistory(String userEmail);
}
