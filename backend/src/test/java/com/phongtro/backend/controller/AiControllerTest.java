package com.phongtro.backend.controller;

import com.phongtro.backend.client.AiServiceClient;
import com.phongtro.backend.common.ApiResponse;
import com.phongtro.backend.dto.ai.*;
import com.phongtro.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiControllerTest {

    @Mock
    private AiServiceClient aiServiceClient;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AiController aiController;

    private FraudCheckRequest sampleFraudRequest;
    private FraudCheckResponse sampleFraudResponse;

    @BeforeEach
    void setUp() {
        sampleFraudRequest = FraudCheckRequest.builder()
                .title("Phòng trọ giá siêu rẻ")
                .description("Chuyển cọc gấp để giữ chỗ")
                .price(1200000.0)
                .area(25.0)
                .district("Quận 1")
                .address("123 Lê Lợi")
                .build();

        sampleFraudResponse = FraudCheckResponse.builder()
                .fraudScore(88.5)
                .riskLevel("high")
                .recommendation("flagged_for_review")
                .reasons(List.of("Giá thấp bất thường", "Yêu cầu cọc gấp"))
                .build();
    }

    @Test
    void checkListingFraud_Success() {
        when(aiServiceClient.checkListingFraud(any(FraudCheckRequest.class)))
                .thenReturn(Optional.of(sampleFraudResponse));

        ResponseEntity<ApiResponse<FraudCheckResponse>> response = aiController.checkListingFraud(sampleFraudRequest);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().isSuccess());
        assertEquals("high", response.getBody().getData().getRiskLevel());
        assertEquals(88.5, response.getBody().getData().getFraudScore());
    }

    @Test
    void checkListingFraud_FallbackWhenServiceDown() {
        when(aiServiceClient.checkListingFraud(any(FraudCheckRequest.class)))
                .thenReturn(Optional.empty());

        ResponseEntity<ApiResponse<FraudCheckResponse>> response = aiController.checkListingFraud(sampleFraudRequest);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals("approved", response.getBody().getData().getRecommendation());
    }

    @Test
    void getRecommendations_Success() {
        RecommendationResponse recResponse = RecommendationResponse.builder()
                .userId(0L)
                .strategy("content_based")
                .recommendations(List.of(
                        RecommendationItemDto.builder()
                                .roomId(101L)
                                .score(0.95)
                                .reason("Cùng quận Quận 7")
                                .build()
                ))
                .build();

        when(aiServiceClient.getRecommendations(eq(0L), eq(10)))
                .thenReturn(Optional.of(recResponse));

        ResponseEntity<ApiResponse<RecommendationResponse>> response = aiController.getRecommendations(null, 10);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().getData().getRecommendations().size());
        assertEquals(101L, response.getBody().getData().getRecommendations().get(0).getRoomId());
    }

    @Test
    void sendChatMessage_Success() {
        ChatbotRequestDto chatReq = ChatbotRequestDto.builder()
                .sessionId("sess-123")
                .message("Phòng này có cho nuôi thú cưng không?")
                .build();

        ChatbotResponseDto chatResp = ChatbotResponseDto.builder()
                .reply("Phòng này cho phép nuôi thú cưng bạn nhé!")
                .intent("pet_policy")
                .confidence(0.96)
                .build();

        when(aiServiceClient.sendChatMessage(any(ChatbotRequestDto.class)))
                .thenReturn(Optional.of(chatResp));

        ResponseEntity<ApiResponse<ChatbotResponseDto>> response = aiController.sendChatMessage(chatReq);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals("pet_policy", response.getBody().getData().getIntent());
        assertTrue(response.getBody().getData().getReply().contains("thú cưng"));
    }

    @Test
    void predictPrice_Success() {
        PricePredictionResponseDto priceResp = PricePredictionResponseDto.builder()
                .district("Quận 7")
                .roomType("phòng trọ")
                .currentAvgPrice(5800000.0)
                .predictedNextMonth(5700000.0)
                .trend("decreasing")
                .confidenceInterval(List.of(5500000.0, 5900000.0))
                .changePercentage(-1.72)
                .factors(List.of("Nguồn cung dồi dào"))
                .build();

        when(aiServiceClient.predictPrice(eq("Quận 7"), eq("phòng trọ")))
                .thenReturn(Optional.of(priceResp));

        ResponseEntity<ApiResponse<PricePredictionResponseDto>> response = aiController.predictPrice("Quận 7", "phòng trọ");

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals("decreasing", response.getBody().getData().getTrend());
        assertEquals(5800000.0, response.getBody().getData().getCurrentAvgPrice());
    }

    @Test
    void getMarketOverview_Success() {
        MarketOverviewResponseDto marketResp = MarketOverviewResponseDto.builder()
                .districts(List.of(
                        DistrictOverviewDto.builder()
                                .district("Bình Thạnh")
                                .avgPrice(7700000.0)
                                .trend("stable")
                                .build()
                ))
                .generatedAt("2026-10-18")
                .build();

        when(aiServiceClient.getMarketOverview())
                .thenReturn(Optional.of(marketResp));

        ResponseEntity<ApiResponse<MarketOverviewResponseDto>> response = aiController.getMarketOverview();

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().getData().getDistricts().size());
        assertEquals("Bình Thạnh", response.getBody().getData().getDistricts().get(0).getDistrict());
    }
}
