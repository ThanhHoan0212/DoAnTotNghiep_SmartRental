package com.phongtro.backend.service.client.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.phongtro.backend.config.FptAiProperties;
import com.phongtro.backend.dto.ekyc.FptAiFaceMatchResult;
import com.phongtro.backend.dto.ekyc.FptAiOcrResult;
import com.phongtro.backend.exception.AppException;
import com.phongtro.backend.exception.ErrorCode;
import com.phongtro.backend.service.client.FptAiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;
import java.time.Duration;
import java.util.Random;

@Slf4j
@Component
@RequiredArgsConstructor
public class FptAiClientImpl implements FptAiClient {

    private final FptAiProperties fptAiProperties;
    private final ObjectMapper objectMapper;
    private final Random random = new Random();

    @Override
    public FptAiOcrResult extractIdCard(MultipartFile frontImage, MultipartFile backImage, String customApiKey) {
        String effectiveApiKey = (customApiKey != null && !customApiKey.trim().isEmpty())
                ? customApiKey.trim()
                : fptAiProperties.getApiKey();

        if (effectiveApiKey != null && !effectiveApiKey.trim().isEmpty()) {
            try {
                log.info("Gửi yêu cầu nhận diện thẻ CCCD tới FPT.AI IDR OCR endpoint: {}", fptAiProperties.getOcrUrl());
                WebClient client = WebClient.builder()
                        .baseUrl(fptAiProperties.getOcrUrl())
                        .build();

                MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
                ByteArrayResource frontResource = new ByteArrayResource(frontImage.getBytes()) {
                    @Override
                    public String getFilename() {
                        return frontImage.getOriginalFilename() != null ? frontImage.getOriginalFilename() : "front.jpg";
                    }
                };
                bodyBuilder.part("image", frontResource, MediaType.IMAGE_JPEG);

                String responseBody = client.post()
                        .header("api-key", effectiveApiKey)
                        .header("api_key", effectiveApiKey)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofSeconds(20))
                        .block();

                log.info("FPT.AI OCR Response: {}", responseBody);
                return parseOcrResponse(responseBody);
            } catch (WebClientResponseException e) {
                String responseErr = e.getResponseBodyAsString();
                log.error("FPT.AI OCR HTTP Error [{}]: {}", e.getStatusCode(), responseErr);
                String detailedMsg = parseErrorMessage(responseErr, e.getMessage());
                if (!fptAiProperties.isMockEnabled()) {
                    throw new AppException(ErrorCode.EKYC_VERIFICATION_FAILED, "Lỗi từ FPT.AI OCR (" + e.getStatusCode().value() + "): " + detailedMsg);
                }
            } catch (AppException ae) {
                throw ae;
            } catch (Exception e) {
                log.error("FPT.AI live OCR request error: {}", e.getMessage());
                if (!fptAiProperties.isMockEnabled()) {
                    throw new AppException(ErrorCode.EKYC_VERIFICATION_FAILED, "Không thể kết nối dịch vụ FPT.AI OCR: " + e.getMessage());
                }
            }
        }

        // Chế độ Mock Sandbox chỉ kích hoạt khi mockEnabled = true
        if (fptAiProperties.isMockEnabled()) {
            return simulateSmartOcr(frontImage, backImage);
        }

        throw new AppException(ErrorCode.EKYC_VERIFICATION_FAILED, "Hệ thống chưa cấu hình FPT.AI API Key hợp lệ.");
    }

    @Override
    public FptAiFaceMatchResult matchFaces(MultipartFile cardImage, MultipartFile selfieImage, String customApiKey, boolean forceMismatch) {
        String effectiveApiKey = (customApiKey != null && !customApiKey.trim().isEmpty())
                ? customApiKey.trim()
                : fptAiProperties.getApiKey();

        if (effectiveApiKey != null && !effectiveApiKey.trim().isEmpty()) {
            try {
                log.info("Gửi yêu cầu so khớp khuôn mặt tới FPT.AI FaceMatch endpoint: {}", fptAiProperties.getFacematchUrl());
                WebClient client = WebClient.builder()
                        .baseUrl(fptAiProperties.getFacematchUrl())
                        .build();

                MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
                ByteArrayResource cardResource = new ByteArrayResource(cardImage.getBytes()) {
                    @Override
                    public String getFilename() {
                        return cardImage.getOriginalFilename() != null ? cardImage.getOriginalFilename() : "card.jpg";
                    }
                };
                ByteArrayResource selfieResource = new ByteArrayResource(selfieImage.getBytes()) {
                    @Override
                    public String getFilename() {
                        return selfieImage.getOriginalFilename() != null ? selfieImage.getOriginalFilename() : "selfie.jpg";
                    }
                };

                bodyBuilder.part("file[]", cardResource, MediaType.IMAGE_JPEG);
                bodyBuilder.part("file[]", selfieResource, MediaType.IMAGE_JPEG);

                String responseBody = client.post()
                        .header("api-key", effectiveApiKey)
                        .header("api_key", effectiveApiKey)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofSeconds(20))
                        .block();

                log.info("FPT.AI FaceMatch Response: {}", responseBody);
                return parseFaceMatchResponse(responseBody);
            } catch (WebClientResponseException e) {
                String responseErr = e.getResponseBodyAsString();
                log.error("FPT.AI FaceMatch HTTP Error [{}]: {}", e.getStatusCode(), responseErr);
                String detailedMsg = parseErrorMessage(responseErr, e.getMessage());
                if (!fptAiProperties.isMockEnabled()) {
                    throw new AppException(ErrorCode.EKYC_VERIFICATION_FAILED, "Lỗi từ FPT.AI FaceMatch (" + e.getStatusCode().value() + "): " + detailedMsg);
                }
            } catch (AppException ae) {
                throw ae;
            } catch (Exception e) {
                log.error("FPT.AI live FaceMatch request error: {}", e.getMessage());
                if (!fptAiProperties.isMockEnabled()) {
                    throw new AppException(ErrorCode.EKYC_VERIFICATION_FAILED, "Không thể kết nối dịch vụ FPT.AI FaceMatch: " + e.getMessage());
                }
            }
        }

        // Chế độ Mock Sandbox chỉ kích hoạt khi mockEnabled = true
        if (fptAiProperties.isMockEnabled()) {
            return simulateSmartFaceMatch(cardImage, selfieImage, forceMismatch);
        }

        throw new AppException(ErrorCode.EKYC_VERIFICATION_FAILED, "Hệ thống chưa cấu hình FPT.AI API Key hợp lệ.");
    }

    private FptAiOcrResult parseOcrResponse(String responseJson) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            if (root.has("errorCode") && root.get("errorCode").asInt() != 0) {
                String errMsg = root.has("errorMessage") ? root.get("errorMessage").asText() : "Lỗi nhận diện thẻ từ FPT.AI";
                throw new AppException(ErrorCode.EKYC_VERIFICATION_FAILED, "FPT.AI OCR báo lỗi: " + errMsg);
            }

            JsonNode dataArray = root.path("data");
            if (dataArray.isArray() && !dataArray.isEmpty()) {
                JsonNode firstCard = dataArray.get(0);

                String idCardNumber = extractField(firstCard, "id");
                String fullName = extractField(firstCard, "name");
                String dob = extractField(firstCard, "dob");
                String sex = extractField(firstCard, "sex");
                String address = extractField(firstCard, "address", "residence");
                String hometown = extractField(firstCard, "home", "origin");
                String issueDate = extractField(firstCard, "issue_date", "doe");
                String cardType = firstCard.path("type").asText("cccd");

                if (idCardNumber.isEmpty()) {
                    throw new AppException(ErrorCode.EKYC_VERIFICATION_FAILED,
                            "FPT.AI không bóc tách được số CCCD trên ảnh. Vui lòng chụp rõ nét, đủ ánh sáng và không bị lóa sáng hay che khuất.");
                }

                return FptAiOcrResult.builder()
                        .idCardNumber(idCardNumber)
                        .fullName(fullName)
                        .dob(dob)
                        .sex(sex)
                        .address(address)
                        .hometown(hometown)
                        .issueDate(issueDate)
                        .cardType(cardType)
                        .rawJson(responseJson)
                        .build();
            }
            throw new AppException(ErrorCode.EKYC_VERIFICATION_FAILED,
                    "Không tìm thấy thông tin CCCD trong phản hồi từ FPT.AI. Vui lòng chụp thẳng góc thẻ và đủ sáng.");
        } catch (AppException ae) {
            throw ae;
        } catch (Exception e) {
            log.error("Lỗi phân tích cú pháp kết quả OCR FPT.AI: {}", e.getMessage());
            throw new AppException(ErrorCode.EKYC_VERIFICATION_FAILED, "Không thể đọc dữ liệu CCCD từ FPT.AI: " + e.getMessage());
        }
    }

    private FptAiFaceMatchResult parseFaceMatchResponse(String responseJson) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            if (root.has("errorCode") && root.get("errorCode").asInt() != 0) {
                String errMsg = root.has("errorMessage") ? root.get("errorMessage").asText() : "Lỗi so khớp khuôn mặt từ FPT.AI";
                throw new AppException(ErrorCode.EKYC_VERIFICATION_FAILED, "FPT.AI FaceMatch báo lỗi: " + errMsg);
            }

            JsonNode data = root.path("data");
            double similarity = data.path("similarity").asDouble(0.0);
            boolean isMatch = data.has("isMatch") ? data.path("isMatch").asBoolean() : (similarity > 85.0);

            return FptAiFaceMatchResult.builder()
                    .similarity(similarity)
                    .isMatch(isMatch)
                    .rawJson(responseJson)
                    .build();
        } catch (AppException ae) {
            throw ae;
        } catch (Exception e) {
            log.error("Lỗi phân tích cú pháp kết quả FaceMatch FPT.AI: {}", e.getMessage());
            throw new AppException(ErrorCode.EKYC_VERIFICATION_FAILED, "Không thể đọc dữ liệu so khớp khuôn mặt từ FPT.AI: " + e.getMessage());
        }
    }

    /**
     * Trích xuất trường dữ liệu từ JSON FPT.AI hỗ trợ cả direct fields và nested info fields
     */
    private String extractField(JsonNode card, String... fieldNames) {
        for (String name : fieldNames) {
            if (card.has(name) && !card.get(name).isNull()) {
                String val = card.get(name).asText();
                if (val != null && !val.trim().isEmpty() && !"N/A".equalsIgnoreCase(val.trim())) {
                    return val.trim();
                }
            }
            JsonNode infoNode = card.path("info");
            if (infoNode.has(name) && !infoNode.get(name).isNull()) {
                String val = infoNode.get(name).asText();
                if (val != null && !val.trim().isEmpty() && !"N/A".equalsIgnoreCase(val.trim())) {
                    return val.trim();
                }
            }
        }
        return "";
    }

    private String parseErrorMessage(String responseBody, String defaultMsg) {
        try {
            if (responseBody != null && !responseBody.trim().isEmpty()) {
                JsonNode root = objectMapper.readTree(responseBody);
                String msg = "";
                if (root.has("errorMessage") && !root.get("errorMessage").asText().isEmpty()) {
                    msg = root.get("errorMessage").asText();
                } else if (root.has("message") && !root.get("message").asText().isEmpty()) {
                    msg = root.get("message").asText();
                }

                if (!msg.isEmpty()) {
                    if (msg.contains("You cannot consume this service")) {
                        return msg + " (API Key chưa được kích hoạt dịch vụ Vision/IDR trên console.fpt.ai. Vui lòng vào console.fpt.ai kích hoạt dịch vụ Nhận dạng CMND/CCCD và So khớp khuôn mặt cho Project của bạn).";
                    }
                    return msg;
                }
            }
        } catch (Exception ignored) {}
        return defaultMsg;
    }

    /**
     * Mô phỏng bóc tách OCR thông minh cho đồ án (khi mockEnabled = true)
     */
    private FptAiOcrResult simulateSmartOcr(MultipartFile frontImage, MultipartFile backImage) {
        log.info("[FPT.AI Smart Sandbox] Processing OCR simulation for image size: {} bytes", frontImage.getSize());

        String mockCccdNumber = "079098123456";

        return FptAiOcrResult.builder()
                .idCardNumber(mockCccdNumber)
                .fullName("NGUYỄN VĂN AN")
                .dob("15/08/1998")
                .sex("Nam")
                .address("Phường 25, Quận Bình Thạnh, TP. Hồ Chí Minh")
                .hometown("TP. Hồ Chí Minh")
                .issueDate("10/05/2021")
                .cardType("chip_front")
                .rawJson("{\"simulation\": true, \"source\": \"FPT.AI Smart Sandbox\", \"errorCode\": 0}")
                .build();
    }

    /**
     * Mô phỏng so khớp khuôn mặt thông minh (> 85% hoặc mô phỏng từ chối khi phát hiện khác khuôn mặt) (khi mockEnabled = true)
     */
    private FptAiFaceMatchResult simulateSmartFaceMatch(MultipartFile cardImage, MultipartFile selfieImage, boolean forceMismatch) {
        String filename = (selfieImage.getOriginalFilename() != null) ? selfieImage.getOriginalFilename().toLowerCase() : "";
        log.info("[FPT.AI Smart Sandbox] Face Matching simulation for selfie: [{}], forceMismatch: {}", filename, forceMismatch);

        if (forceMismatch || filename.contains("fail") || filename.contains("reject") || filename.contains("khac") || filename.contains("diff")) {
            double lowScore = 45.0 + random.nextDouble() * 25.0;
            double roundedLow = Math.round(lowScore * 10.0) / 10.0;
            return FptAiFaceMatchResult.builder()
                    .similarity(roundedLow)
                    .isMatch(false)
                    .rawJson("{\"simulation\": true, \"isMatch\": false, \"similarity\": " + roundedLow + "}")
                    .build();
        }

        double highScore = 89.0 + random.nextDouble() * 8.5;
        double roundedScore = Math.round(highScore * 10.0) / 10.0;

        return FptAiFaceMatchResult.builder()
                .similarity(roundedScore)
                .isMatch(true)
                .rawJson("{\"simulation\": true, \"isMatch\": true, \"similarity\": " + roundedScore + "}")
                .build();
    }
}
