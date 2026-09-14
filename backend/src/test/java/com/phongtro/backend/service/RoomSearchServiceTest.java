package com.phongtro.backend.service;

import com.phongtro.backend.client.AiServiceClient;
import com.phongtro.backend.common.PageResponse;
import com.phongtro.backend.dto.request.NlpSearchRequest;
import com.phongtro.backend.dto.request.RoomSearchRequest;
import com.phongtro.backend.dto.response.DistrictCountResponse;
import com.phongtro.backend.dto.response.NlpSearchResponse;
import com.phongtro.backend.dto.response.ParsedQueryResponse;
import com.phongtro.backend.dto.response.RoomSummaryResponse;
import com.phongtro.backend.entity.Room;
import com.phongtro.backend.entity.RoomStatus;
import com.phongtro.backend.mapper.RoomMapper;
import com.phongtro.backend.repository.RoomRepository;
import com.phongtro.backend.service.impl.RoomSearchServiceImpl;
import com.phongtro.backend.service.parser.RuleBasedQueryParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomSearchServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomMapper roomMapper;

    @Mock
    private AiServiceClient aiServiceClient;

    @Mock
    private RuleBasedQueryParser ruleBasedQueryParser;

    @InjectMocks
    private RoomSearchServiceImpl roomSearchService;

    private Room sampleRoom;
    private RoomSummaryResponse sampleSummary;

    @BeforeEach
    void setUp() {
        sampleRoom = Room.builder()
                .id(UUID.randomUUID())
                .title("Phòng trọ Bình Thạnh đầy đủ tiện nghi")
                .price(3500000.0)
                .area(25.0)
                .address("123 Điện Biên Phủ")
                .district("Bình Thạnh")
                .city("Hồ Chí Minh")
                .status(RoomStatus.APPROVED)
                .build();

        sampleSummary = RoomSummaryResponse.builder()
                .id(sampleRoom.getId())
                .title(sampleRoom.getTitle())
                .price(sampleRoom.getPrice())
                .area(sampleRoom.getArea())
                .district(sampleRoom.getDistrict())
                .city(sampleRoom.getCity())
                .status(RoomStatus.APPROVED)
                .build();
    }

    @Test
    @DisplayName("Tìm kiếm đa tiêu chí thành công")
    void searchRooms_MultiCriteria_Success() {
        RoomSearchRequest request = RoomSearchRequest.builder()
                .district("Bình Thạnh")
                .minPrice(2000000.0)
                .maxPrice(4000000.0)
                .minArea(20.0)
                .sortBy("price")
                .sortDirection("asc")
                .page(0)
                .size(10)
                .build();

        Page<Room> page = new PageImpl<>(List.of(sampleRoom));
        when(roomRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(roomMapper.toRoomSummaryResponse(sampleRoom)).thenReturn(sampleSummary);

        PageResponse<RoomSummaryResponse> result = roomSearchService.searchRooms(request);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals("Bình Thạnh", result.getContent().get(0).getDistrict());
        verify(roomRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("Tìm kiếm bằng ngôn ngữ tự nhiên - AI Service phản hồi thành công")
    void searchByNaturalLanguage_AiServiceSuccess() {
        String query = "tìm phòng trọ ở Bình Thạnh dưới 4 triệu có điều hòa";
        NlpSearchRequest nlpRequest = NlpSearchRequest.builder()
                .queryText(query)
                .page(0)
                .size(10)
                .build();

        ParsedQueryResponse aiParsed = ParsedQueryResponse.builder()
                .originalQuery(query)
                .district("Bình Thạnh")
                .maxPrice(4000000.0)
                .detectedAmenities(List.of("Điều hòa nhiệt độ"))
                .amenityIds(List.of(1L))
                .source("AI_SERVICE")
                .build();

        when(aiServiceClient.parseNaturalLanguageQuery(query)).thenReturn(Optional.of(aiParsed));

        Page<Room> page = new PageImpl<>(List.of(sampleRoom));
        when(roomRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(roomMapper.toRoomSummaryResponse(sampleRoom)).thenReturn(sampleSummary);

        NlpSearchResponse response = roomSearchService.searchByNaturalLanguage(nlpRequest);

        assertNotNull(response);
        assertEquals("AI_SERVICE", response.getParsedQuery().getSource());
        assertEquals("Bình Thạnh", response.getParsedQuery().getDistrict());
        assertEquals(4000000.0, response.getParsedQuery().getMaxPrice());
        assertEquals(1, response.getResults().getTotalElements());
        verify(aiServiceClient, times(1)).parseNaturalLanguageQuery(query);
        verify(ruleBasedQueryParser, never()).parse(any());
    }

    @Test
    @DisplayName("Tìm kiếm bằng ngôn ngữ tự nhiên - AI Service offline -> Tự động Fallback sang Rule-Based Parser")
    void searchByNaturalLanguage_FallbackToRuleBased() {
        String query = "tìm phòng trọ khép kín Bình Thạnh dưới 4tr";
        NlpSearchRequest nlpRequest = NlpSearchRequest.builder()
                .queryText(query)
                .page(0)
                .size(10)
                .build();

        ParsedQueryResponse fallbackParsed = ParsedQueryResponse.builder()
                .originalQuery(query)
                .district("Bình Thạnh")
                .maxPrice(4000000.0)
                .detectedAmenities(List.of("Vệ sinh khép kín"))
                .source("RULE_BASED_FALLBACK")
                .build();

        when(aiServiceClient.parseNaturalLanguageQuery(query)).thenReturn(Optional.empty());
        when(ruleBasedQueryParser.parse(query)).thenReturn(fallbackParsed);

        Page<Room> page = new PageImpl<>(List.of(sampleRoom));
        when(roomRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(roomMapper.toRoomSummaryResponse(sampleRoom)).thenReturn(sampleSummary);

        NlpSearchResponse response = roomSearchService.searchByNaturalLanguage(nlpRequest);

        assertNotNull(response);
        assertEquals("RULE_BASED_FALLBACK", response.getParsedQuery().getSource());
        assertEquals("Bình Thạnh", response.getParsedQuery().getDistrict());
        verify(aiServiceClient, times(1)).parseNaturalLanguageQuery(query);
        verify(ruleBasedQueryParser, times(1)).parse(query);
    }

    @Test
    @DisplayName("Thống kê số lượng phòng theo quận huyện thành công")
    void getDistrictRoomCounts_Success() {
        List<DistrictCountResponse> counts = List.of(
                new DistrictCountResponse("Bình Thạnh", 15L),
                new DistrictCountResponse("Quận 1", 10L)
        );
        when(roomRepository.countRoomsByDistrict()).thenReturn(counts);

        List<DistrictCountResponse> result = roomSearchService.getDistrictRoomCounts();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Bình Thạnh", result.get(0).getDistrict());
        assertEquals(15L, result.get(0).getCount());
        verify(roomRepository, times(1)).countRoomsByDistrict();
    }
}
