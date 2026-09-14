package com.phongtro.backend.service;

import com.phongtro.backend.common.PageResponse;
import com.phongtro.backend.dto.request.NlpSearchRequest;
import com.phongtro.backend.dto.request.RoomSearchRequest;
import com.phongtro.backend.dto.response.DistrictCountResponse;
import com.phongtro.backend.dto.response.NlpSearchResponse;
import com.phongtro.backend.dto.response.ParsedQueryResponse;
import com.phongtro.backend.dto.response.RoomSummaryResponse;

import java.util.List;

public interface RoomSearchService {

    /**
     * Tìm kiếm phòng trọ đa tiêu chí (keyword, giá, diện tích, quận, tiện ích, bán kính tọa độ)
     */
    PageResponse<RoomSummaryResponse> searchRooms(RoomSearchRequest request);

    /**
     * Tìm kiếm thông minh bằng ngôn ngữ tự nhiên (NLP) tích hợp AI Service + Fallback Rule-Based Parser
     */
    NlpSearchResponse searchByNaturalLanguage(NlpSearchRequest request);

    /**
     * Phân tích câu truy vấn tự nhiên thành các tiêu chí lọc có cấu trúc
     */
    ParsedQueryResponse parseNaturalLanguageQuery(String queryText);

    /**
     * Thống kê số lượng phòng trọ đang cho thuê theo từng quận / huyện
     */
    List<DistrictCountResponse> getDistrictRoomCounts();
}
