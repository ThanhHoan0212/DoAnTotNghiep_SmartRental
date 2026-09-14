package com.phongtro.backend.client;

import com.phongtro.backend.dto.response.ParsedQueryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiServiceClient {

    private final WebClient aiServiceWebClient;

    /**
     * Gửi câu truy vấn tìm kiếm tự nhiên sang Python FastAPI để phân tích NLP
     * Endpoint: POST /ai/nlp/parse-query
     */
    public Optional<ParsedQueryResponse> parseNaturalLanguageQuery(String queryText) {
        try {
            log.info("Calling AI Service (FastAPI) to parse query: [{}]", queryText);

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
            log.warn("AI Service unavailable or timed out: {}. Activating internal Rule-Based Parser fallback.", ex.getMessage());
        }
        return Optional.empty();
    }
}
