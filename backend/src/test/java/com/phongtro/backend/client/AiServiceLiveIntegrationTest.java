package com.phongtro.backend.client;

import com.phongtro.backend.config.WebClientConfig;
import com.phongtro.backend.dto.ai.*;
import com.phongtro.backend.dto.response.ParsedQueryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AiServiceLiveIntegrationTest {

    private AiServiceClient aiServiceClient;

    @BeforeEach
    void setUp() {
        WebClientConfig config = new WebClientConfig();
        ReflectionTestUtils.setField(config, "aiServiceBaseUrl", "http://127.0.0.1:8000");
        ReflectionTestUtils.setField(config, "timeoutSeconds", 5);
        ReflectionTestUtils.setField(config, "aiServiceApiKey", "smartrental-ai-secret-key-2026");

        WebClient webClient = config.aiServiceWebClient();
        aiServiceClient = new AiServiceClient(webClient);
    }

    @Test
    void testLiveNlpParseQuery() {
        Optional<ParsedQueryResponse> result = aiServiceClient.parseNaturalLanguageQuery("tìm phòng quận 7 dưới 5 triệu");
        if (result.isPresent()) {
            ParsedQueryResponse parsed = result.get();
            assertEquals("AI_SERVICE", parsed.getSource());
            assertEquals("Quận 7", parsed.getDistrict());
            assertEquals(5000000.0, parsed.getMaxPrice());
        }
    }

    @Test
    void testLiveFraudCheck() {
        FraudCheckRequest req = FraudCheckRequest.builder()
                .title("Phòng trọ giá sốc đặt cọc liền")
                .description("Chuyển cọc qua zalo gấp")
                .price(1000000.0)
                .area(30.0)
                .district("Quận 1")
                .address("123 Lê Duẩn")
                .build();

        Optional<FraudCheckResponse> result = aiServiceClient.checkListingFraud(req);
        assertTrue(result.isPresent(), "Expected live fraud check response");
        FraudCheckResponse fraud = result.get();
        assertNotNull(fraud.getFraudScore());
        assertNotNull(fraud.getRiskLevel());
        assertNotNull(fraud.getIsFraud());
        assertFalse(fraud.getReasons().isEmpty());
    }

    @Test
    void testLivePricePrediction() {
        Optional<PricePredictionResponseDto> result = aiServiceClient.predictPrice("Quận 7", "phòng trọ");
        assertTrue(result.isPresent(), "Expected live price prediction response");
        PricePredictionResponseDto price = result.get();
        assertEquals("Quận 7", price.getDistrict());
        assertNotNull(price.getCurrentAvgPrice());
        assertNotNull(price.getPredictedNextMonth());
        assertNotNull(price.getTrend());
    }

    @Test
    void testLiveChatbotRespond() {
        ChatbotRequestDto req = ChatbotRequestDto.builder()
                .sessionId("test-integration-session")
                .message("phòng này có cho nuôi chó mèo không")
                .build();

        Optional<ChatbotResponseDto> result = aiServiceClient.sendChatMessage(req);
        assertTrue(result.isPresent(), "Expected live chatbot response");
        ChatbotResponseDto chat = result.get();
        assertNotNull(chat.getReply());
        assertEquals("ask_pet_policy", chat.getIntent());
    }
}
