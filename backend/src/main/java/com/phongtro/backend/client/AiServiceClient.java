package com.phongtro.backend.client;

import com.phongtro.backend.dto.ai.*;
import com.phongtro.backend.dto.response.ParsedQueryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiServiceClient {

    private final WebClient aiServiceWebClient;

    /**
     * Module 4: Gửi câu truy vấn tìm kiếm tự nhiên sang Python FastAPI để phân tích NLP
     * Endpoint: POST /ai/nlp/parse-query
     */
    public Optional<ParsedQueryResponse> parseNaturalLanguageQuery(String queryText) {
        try {
            log.info("Calling AI Service [POST /ai/nlp/parse-query] query: [{}]", queryText);

            ParsedQueryResponse response = aiServiceWebClient.post()
                    .uri("/ai/nlp/parse-query")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("query", queryText))
                    .retrieve()
                    .bodyToMono(ParsedQueryResponse.class)
                    .block();

            if (response != null) {
                response.setSource("AI_SERVICE");
                response.setOriginalQuery(queryText);
                return Optional.of(response);
            }
        } catch (Exception ex) {
            log.warn("AI Service NLP parse-query failed: {}. Activating Rule-Based Parser fallback.", ex.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Module 2: Kiểm tra tin đăng có dấu hiệu lừa đảo / bất thường không
     * Endpoint: POST /ai/fraud/check-listing
     */
    public Optional<FraudCheckResponse> checkListingFraud(FraudCheckRequest request) {
        try {
            log.info("Calling AI Service [POST /ai/fraud/check-listing] title: [{}]", request.getTitle());

            FraudCheckResponse response = aiServiceWebClient.post()
                    .uri("/ai/fraud/check-listing")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(FraudCheckResponse.class)
                    .block();

            return Optional.ofNullable(response);
        } catch (Exception ex) {
            log.warn("AI Service fraud check-listing failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Module 2: Đồng bộ tin đăng vào kho dữ liệu chống trùng lặp
     * Endpoint: POST /ai/fraud/sync-listing
     */
    public void syncListing(Map<String, Object> listingData) {
        try {
            aiServiceWebClient.post()
                    .uri("/ai/fraud/sync-listing")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(listingData)
                    .retrieve()
                    .toBodilessEntity()
                    .subscribe(
                            success -> log.debug("Synced listing to AI fraud database successfully"),
                            err -> log.warn("Failed to sync listing to AI fraud database: {}", err.getMessage())
                    );
        } catch (Exception ex) {
            log.warn("Error triggering syncListing to AI service: {}", ex.getMessage());
        }
    }

    /**
     * Module 3: Lấy danh sách gợi ý phòng trọ cho người dùng
     * Endpoint: GET /ai/recommendation/{user_id}?limit={limit}
     */
    public Optional<RecommendationResponse> getRecommendations(Object userId, Integer limit) {
        try {
            int finalLimit = (limit != null && limit > 0) ? limit : 10;
            String userStr = (userId != null) ? String.valueOf(userId) : "0";
            log.info("Calling AI Service [GET /ai/recommendation/{}] limit: {}", userStr, finalLimit);

            RecommendationResponse response = aiServiceWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/ai/recommendation/{user_id}")
                            .queryParam("limit", finalLimit)
                            .build(userStr))
                    .retrieve()
                    .bodyToMono(RecommendationResponse.class)
                    .block();

            return Optional.ofNullable(response);
        } catch (Exception ex) {
            log.warn("AI Service recommendation failed for user {}: {}", userId, ex.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Module 3: Đồng bộ đặc trưng phòng trọ vào không gian vector AI Recommendation
     * Endpoint: POST /ai/recommendation/sync-room
     */
    public void syncRoomFeature(Map<String, Object> roomData) {
        try {
            aiServiceWebClient.post()
                    .uri("/ai/recommendation/sync-room")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(roomData)
                    .retrieve()
                    .toBodilessEntity()
                    .subscribe(
                            success -> log.debug("Synced room to AI recommendation successfully"),
                            err -> log.warn("Failed to sync room to AI recommendation: {}", err.getMessage())
                    );
        } catch (Exception ex) {
            log.warn("Error triggering syncRoomFeature to AI service: {}", ex.getMessage());
        }
    }

    /**
     * Module 3: Lấy gợi ý phòng trọ cá nhân hóa kèm context (phòng yêu thích / đang xem)
     * Endpoint: POST /ai/recommendation/personalized
     */
    public Optional<RecommendationResponse> getPersonalizedRecommendations(PersonalizedRecommendRequest request) {
        try {
            log.info("Calling AI Service [POST /ai/recommendation/personalized] user: {}", request.getUserId());

            RecommendationResponse response = aiServiceWebClient.post()
                    .uri("/ai/recommendation/personalized")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(RecommendationResponse.class)
                    .block();

            return Optional.ofNullable(response);
        } catch (Exception ex) {
            log.warn("AI Service personalized recommendation failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Module 3: Đồng bộ tương tác người dùng (view, favorite, contact)
     * Endpoint: POST /ai/recommendation/sync-interaction
     */
    public void syncUserInteraction(UserInteractionSyncRequest request) {
        try {
            aiServiceWebClient.post()
                    .uri("/ai/recommendation/sync-interaction")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .toBodilessEntity()
                    .subscribe(
                            success -> log.debug("Synced interaction to AI recommendation successfully"),
                            err -> log.warn("Failed to sync interaction to AI: {}", err.getMessage())
                    );
        } catch (Exception ex) {
            log.warn("Error triggering syncUserInteraction to AI service: {}", ex.getMessage());
        }
    }

    /**
     * Module 5: Gửi tin nhắn đến Chatbot AI tư vấn
     * Endpoint: POST /ai/chatbot/respond
     */
    public Optional<ChatbotResponseDto> sendChatMessage(ChatbotRequestDto request) {
        try {
            log.info("Calling AI Service [POST /ai/chatbot/respond] session: [{}]", request.getSessionId());

            ChatbotResponseDto response = aiServiceWebClient.post()
                    .uri("/ai/chatbot/respond")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(ChatbotResponseDto.class)
                    .block();

            return Optional.ofNullable(response);
        } catch (Exception ex) {
            log.warn("AI Service chatbot respond failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Module 5: Lấy lịch sử hội thoại của session
     * Endpoint: GET /ai/chatbot/history/{session_id}
     */
    public Optional<List<Map<String, Object>>> getChatbotHistory(String sessionId) {
        try {
            log.info("Calling AI Service [GET /ai/chatbot/history/{}]", sessionId);

            List<Map<String, Object>> response = aiServiceWebClient.get()
                    .uri("/ai/chatbot/history/{session_id}", sessionId)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                    .block();

            return Optional.ofNullable(response);
        } catch (Exception ex) {
            log.warn("AI Service chatbot history failed for session {}: {}", sessionId, ex.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Module 6: Dự đoán xu hướng giá thuê của quận
     * Endpoint: GET /ai/price-prediction?district={district}&room_type={roomType}
     */
    public Optional<PricePredictionResponseDto> predictPrice(String district, String roomType) {
        try {
            log.info("Calling AI Service [GET /ai/price-prediction] district: [{}], roomType: [{}]", district, roomType);

            PricePredictionResponseDto response = aiServiceWebClient.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder.path("/ai/price-prediction")
                                .queryParam("district", district);
                        if (roomType != null && !roomType.isBlank()) {
                            builder.queryParam("room_type", roomType);
                        }
                        return builder.build();
                    })
                    .retrieve()
                    .bodyToMono(PricePredictionResponseDto.class)
                    .block();

            return Optional.ofNullable(response);
        } catch (Exception ex) {
            log.warn("AI Service price-prediction failed for district {}: {}", district, ex.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Module 6: Lấy tổng quan thị trường các quận
     * Endpoint: GET /ai/price-prediction/market-overview
     */
    public Optional<MarketOverviewResponseDto> getMarketOverview() {
        try {
            log.info("Calling AI Service [GET /ai/price-prediction/market-overview]");

            MarketOverviewResponseDto response = aiServiceWebClient.get()
                    .uri("/ai/price-prediction/market-overview")
                    .retrieve()
                    .bodyToMono(MarketOverviewResponseDto.class)
                    .block();

            return Optional.ofNullable(response);
        } catch (Exception ex) {
            log.warn("AI Service market-overview failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }
}
