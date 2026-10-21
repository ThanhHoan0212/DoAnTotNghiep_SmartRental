package com.phongtro.backend.controller;

import com.phongtro.backend.client.AiServiceClient;
import com.phongtro.backend.common.ApiResponse;
import com.phongtro.backend.dto.ai.*;
import com.phongtro.backend.entity.User;
import com.phongtro.backend.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Tag(name = "7. AI Smart Services", description = "Tích hợp dịch vụ AI: Gợi ý phòng trọ, Chatbot tư vấn, Dự đoán giá & Kiểm duyệt tin đăng")
public class AiController {

    private final AiServiceClient aiServiceClient;
    private final UserRepository userRepository;

    @PostMapping("/fraud/check")
    @Operation(summary = "Kiểm tra rủi ro gian lận tin đăng",
            description = "Gửi thông tin tin đăng để AI phân tích phát hiện tin ảo, giá quá thấp bất thường hoặc lừa đảo cọc")
    public ResponseEntity<ApiResponse<FraudCheckResponse>> checkListingFraud(
            @Valid @RequestBody FraudCheckRequest request) {
        return aiServiceClient.checkListingFraud(request)
                .map(res -> ResponseEntity.ok(ApiResponse.success("Kiểm tra gian lận thành công", res)))
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.success("AI Service tạm thời bận, áp dụng kiểm duyệt thông thường",
                        FraudCheckResponse.builder()
                                .fraudScore(10.0)
                                .riskLevel("low")
                                .recommendation("approved")
                                .reasons(List.of("Fallback kiểm duyệt mặc định do AI Service không phản hồi"))
                                .build())));
    }

    @GetMapping("/recommendations")
    @Operation(summary = "Lấy danh sách phòng trọ gợi ý cho người dùng",
            description = "Gợi ý cá nhân hóa dựa trên lịch sử tương tác, yêu thích hoặc phòng nổi bật (Cold Start)")
    public ResponseEntity<ApiResponse<RecommendationResponse>> getRecommendations(
            Authentication authentication,
            @RequestParam(defaultValue = "10") Integer limit) {

        Long userId = 0L;
        if (authentication != null && authentication.isAuthenticated()) {
            User user = userRepository.findByEmail(authentication.getName()).orElse(null);
            if (user != null) {
                // Sử dụng hash code dương làm numeric id cho hệ thống gợi ý AI nếu user entity dùng UUID
                userId = Math.abs((long) user.getId().hashCode());
            }
        }

        return aiServiceClient.getRecommendations(userId, limit)
                .map(res -> ResponseEntity.ok(ApiResponse.success("Lấy gợi ý phòng trọ thành công", res)))
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.success("Lấy gợi ý mặc định",
                        RecommendationResponse.builder()
                                .userId(0L)
                                .strategy("default_fallback")
                                .recommendations(List.of())
                                .build())));
    }

    @PostMapping("/recommendations/personalized")
    @Operation(summary = "Lấy gợi ý cá nhân hóa theo ngữ cảnh",
            description = "Gợi ý dựa trên danh sách phòng đang xem và danh sách phòng yêu thích hiện thời")
    public ResponseEntity<ApiResponse<RecommendationResponse>> getPersonalizedRecommendations(
            @RequestBody PersonalizedRecommendRequest request) {
        return aiServiceClient.getPersonalizedRecommendations(request)
                .map(res -> ResponseEntity.ok(ApiResponse.success("Lấy gợi ý cá nhân hóa thành công", res)))
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.success("Gợi ý dự phòng",
                        RecommendationResponse.builder()
                                .userId(request.getUserId())
                                .strategy("fallback")
                                .recommendations(List.of())
                                .build())));
    }

    @PostMapping("/chatbot/message")
    @Operation(summary = "Trò chuyện với trợ lý ảo AI",
            description = "Chatbot tư vấn giá cả, tiện ích, nội quy, chính sách nuôi thú cưng và gợi ý phòng trọ theo nhu cầu")
    public ResponseEntity<ApiResponse<ChatbotResponseDto>> sendChatMessage(
            @Valid @RequestBody ChatbotRequestDto request) {
        return aiServiceClient.sendChatMessage(request)
                .map(res -> ResponseEntity.ok(ApiResponse.success(res)))
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.success(
                        ChatbotResponseDto.builder()
                                .reply("Xin lỗi bạn, trợ lý AI hiện đang bảo trì. Vui lòng thử lại sau ít phút hoặc liên hệ trực tiếp chủ nhà nhé!")
                                .intent("fallback")
                                .confidence(0.5)
                                .suggestedRooms(List.of())
                                .build())));
    }

    @GetMapping("/chatbot/history/{sessionId}")
    @Operation(summary = "Lấy lịch sử hội thoại của session",
            description = "Tải lại các tin nhắn đã trao đổi trong phiên tư vấn")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getChatbotHistory(
            @PathVariable String sessionId) {
        return aiServiceClient.getChatbotHistory(sessionId)
                .map(res -> ResponseEntity.ok(ApiResponse.success(res)))
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.success(List.of())));
    }

    @GetMapping("/price-prediction")
    @Operation(summary = "Dự đoán xu hướng giá thuê của quận",
            description = "Dự báo mức giá thuê trung bình tháng kế tiếp kèm khoảng tin cậy 95% và xu hướng tăng/giảm")
    public ResponseEntity<ApiResponse<PricePredictionResponseDto>> predictPrice(
            @RequestParam String district,
            @RequestParam(required = false, defaultValue = "phòng trọ") String roomType) {
        return aiServiceClient.predictPrice(district, roomType)
                .map(res -> ResponseEntity.ok(ApiResponse.success(res)))
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.success(
                        PricePredictionResponseDto.builder()
                                .district(district)
                                .roomType(roomType)
                                .currentAvgPrice(4500000.0)
                                .predictedNextMonth(4500000.0)
                                .trend("stable")
                                .confidenceInterval(List.of(4300000.0, 4700000.0))
                                .changePercentage(0.0)
                                .factors(List.of("Dữ liệu tạm thời từ thống kê cơ bản"))
                                .build())));
    }

    @GetMapping("/price-prediction/market-overview")
    @Operation(summary = "Tổng quan thị trường giá thuê TP.HCM",
            description = "Báo cáo mức giá trung bình, giá trần, giá sàn và xu hướng của 11 quận trọng điểm")
    public ResponseEntity<ApiResponse<MarketOverviewResponseDto>> getMarketOverview() {
        return aiServiceClient.getMarketOverview()
                .map(res -> ResponseEntity.ok(ApiResponse.success(res)))
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.success(
                        MarketOverviewResponseDto.builder()
                                .districts(List.of())
                                .generatedAt("2026-10-18")
                                .build())));
    }
}
