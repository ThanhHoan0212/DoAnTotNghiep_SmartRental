package com.phongtro.backend.service.impl;

import com.phongtro.backend.client.AiServiceClient;
import com.phongtro.backend.common.PageResponse;
import com.phongtro.backend.dto.request.NlpSearchRequest;
import com.phongtro.backend.dto.request.RoomSearchRequest;
import com.phongtro.backend.dto.response.DistrictCountResponse;
import com.phongtro.backend.dto.response.NlpSearchResponse;
import com.phongtro.backend.dto.response.ParsedQueryResponse;
import com.phongtro.backend.dto.response.RoomSummaryResponse;
import com.phongtro.backend.entity.Room;
import com.phongtro.backend.mapper.RoomMapper;
import com.phongtro.backend.repository.RoomRepository;
import com.phongtro.backend.repository.specification.RoomSpecification;
import com.phongtro.backend.service.RoomSearchService;
import com.phongtro.backend.service.parser.RuleBasedQueryParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomSearchServiceImpl implements RoomSearchService {

    private final RoomRepository roomRepository;
    private final RoomMapper roomMapper;
    private final AiServiceClient aiServiceClient;
    private final RuleBasedQueryParser ruleBasedQueryParser;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "price", "area", "viewCount");

    @Override
    public PageResponse<RoomSummaryResponse> searchRooms(RoomSearchRequest request) {
        if (request == null) {
            request = new RoomSearchRequest();
        }

        // 1. Chuẩn hóa phân trang & sắp xếp
        int page = Math.max(0, request.getPage());
        int size = request.getSize() > 0 ? request.getSize() : 10;

        String sortBy = request.getSortBy();
        if (sortBy == null || !ALLOWED_SORT_FIELDS.contains(sortBy)) {
            sortBy = "createdAt";
        }

        Sort.Direction direction = "asc".equalsIgnoreCase(request.getSortDirection())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        // 2. Thực hiện truy vấn với JPA Criteria Specification
        Page<Room> roomPage = roomRepository.findAll(
                RoomSpecification.buildSearchSpecification(request),
                pageable
        );

        // 3. Map sang DTO Response
        Page<RoomSummaryResponse> responsePage = roomPage.map(roomMapper::toRoomSummaryResponse);

        return PageResponse.from(responsePage);
    }

    @Override
    public NlpSearchResponse searchByNaturalLanguage(NlpSearchRequest request) {
        log.info("Processing Natural Language Room Search: [{}]", request.getQueryText());

        // 1. Phân tích câu truy vấn (AI Service với Fallback Rule-Based Parser)
        ParsedQueryResponse parsed = parseNaturalLanguageQuery(request.getQueryText());

        // 2. Chuyển đổi thông tin đã trích xuất thành RoomSearchRequest
        RoomSearchRequest searchRequest = RoomSearchRequest.builder()
                .keyword(parsed.getKeyword())
                .district(parsed.getDistrict())
                .minPrice(parsed.getMinPrice())
                .maxPrice(parsed.getMaxPrice())
                .minArea(parsed.getMinArea())
                .maxArea(parsed.getMaxArea())
                .amenityIds(parsed.getAmenityIds() != null ? parsed.getAmenityIds() : Collections.emptyList())
                .sortBy("createdAt")
                .sortDirection("desc")
                .page(request.getPage())
                .size(request.getSize())
                .build();

        // 3. Thực thi tìm kiếm
        PageResponse<RoomSummaryResponse> searchResults = searchRooms(searchRequest);

        return NlpSearchResponse.builder()
                .parsedQuery(parsed)
                .results(searchResults)
                .build();
    }

    @Override
    public ParsedQueryResponse parseNaturalLanguageQuery(String queryText) {
        if (queryText == null || queryText.trim().isBlank()) {
            return ParsedQueryResponse.builder()
                    .originalQuery("")
                    .source("RULE_BASED_FALLBACK")
                    .build();
        }

        // Ưu tiên gọi AI Service (Python FastAPI), nếu lỗi hoặc timeout thì fallback sang RuleBasedQueryParser
        return aiServiceClient.parseNaturalLanguageQuery(queryText)
                .orElseGet(() -> ruleBasedQueryParser.parse(queryText));
    }

    @Override
    public List<DistrictCountResponse> getDistrictRoomCounts() {
        return roomRepository.countRoomsByDistrict();
    }
}
