package com.phongtro.backend.service.client.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.phongtro.backend.config.VnptEkycProperties;
import com.phongtro.backend.dto.ekyc.VnptFaceMatchResult;
import com.phongtro.backend.dto.ekyc.VnptOcrResult;
import com.phongtro.backend.exception.AppException;
import com.phongtro.backend.exception.ErrorCode;
import com.phongtro.backend.service.client.VnptEkycClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class VnptEkycClientImpl implements VnptEkycClient {

    private final VnptEkycProperties vnptEkycProperties;
    private final ObjectMapper objectMapper;
    private final Random random = new Random();

    @Override
    public VnptOcrResult extractIdCard(MultipartFile frontImage, MultipartFile backImage, String customToken) {
        if (vnptEkycProperties.isMockEnabled()) {
            log.info("[MOCK] Đang sử dụng chế độ giả lập VNPT eKYC OCR (mock-enabled=true)");
            return simulateVnptOcr(frontImage, backImage);
        }

        boolean hasCreds = vnptEkycProperties.hasCredentials() || (customToken != null && !customToken.trim().isEmpty());
        if (!hasCreds) {
            throw new AppException(ErrorCode.EKYC_VERIFICATION_FAILED, "Hệ thống chưa tìm thấy Token ID / Token Key / Access Token của VNPT eKYC trong cấu hình (.env hoặc application.yml).");
        }

        try {
            // Bước 1: Upload file ảnh mặt trước (và mặt sau nếu có) lên VNPT File Service để lấy file hash
            String frontHash = uploadImageToFileService(frontImage, customToken);
            String backHash = (backImage != null && !backImage.isEmpty()) ? uploadImageToFileService(backImage, customToken) : null;

            // Bước 2: Xác định Endpoint bóc tách OCR
            String targetOcrUrl = vnptEkycProperties.getOcrUrl();
            if (backHash != null && targetOcrUrl.endsWith("/front")) {
                targetOcrUrl = targetOcrUrl.substring(0, targetOcrUrl.length() - "/front".length());
            }

            log.info("Gửi yêu cầu bóc tách thẻ CCCD tới VNPT eKYC OCR: {}", targetOcrUrl);
            WebClient client = WebClient.builder()
                    .baseUrl(targetOcrUrl)
                    .build();

            Map<String, Object> reqBody = new HashMap<>();
            reqBody.put("img_front", frontHash);
            if (backHash != null) {
                reqBody.put("img_back", backHash);
            }
            reqBody.put("client_session", "session_" + UUID.randomUUID().toString());
            reqBody.put("token", "token_" + UUID.randomUUID().toString());

            String responseBody = client.post()
                    .headers(headers -> applyVnptHeaders(headers, customToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(reqBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(25))
                    .block();

            log.info("VNPT eKYC OCR Response: {}", responseBody);
            return parseOcrResponse(responseBody);
        } catch (WebClientResponseException e) {
            String responseErr = e.getResponseBodyAsString();
            log.error("VNPT eKYC OCR HTTP Error [{}]: {}", e.getStatusCode(), responseErr);
            String detailedMsg = parseErrorMessage(responseErr, e.getMessage());
            throw new AppException(ErrorCode.EKYC_VERIFICATION_FAILED, "Lỗi từ VNPT eKYC OCR (" + e.getStatusCode().value() + "): " + detailedMsg);
        } catch (AppException ae) {
            throw ae;
        } catch (Exception e) {
            log.error("VNPT eKYC OCR request error: {}", e.getMessage());
            throw new AppException(ErrorCode.EKYC_VERIFICATION_FAILED, "Không thể kết nối dịch vụ VNPT eKYC OCR: " + e.getMessage());
        }
    }

    @Override
    public VnptFaceMatchResult matchFaces(MultipartFile cardImage, MultipartFile selfieImage, String customToken, boolean forceMismatch) {
        if (vnptEkycProperties.isMockEnabled()) {
            log.info("[MOCK] Đang sử dụng chế độ giả lập VNPT eKYC Face Compare (mock-enabled=true)");
            return simulateVnptFaceMatch(cardImage, selfieImage, forceMismatch);
        }

        boolean hasCreds = vnptEkycProperties.hasCredentials() || (customToken != null && !customToken.trim().isEmpty());
        if (!hasCreds) {
            throw new AppException(ErrorCode.EKYC_VERIFICATION_FAILED, "Hệ thống chưa tìm thấy Token ID / Token Key / Access Token của VNPT eKYC trong cấu hình (.env hoặc application.yml).");
        }

        try {
            // Bước 1: Upload ảnh thẻ CCCD và ảnh chân dung selfie lên VNPT File Service
            String cardHash = uploadImageToFileService(cardImage, customToken);
            String faceHash = uploadImageToFileService(selfieImage, customToken);

            log.info("Gửi yêu cầu so khớp khuôn mặt tới VNPT eKYC Face Compare: {}", vnptEkycProperties.getFacematchUrl());
            WebClient client = WebClient.builder()
                    .baseUrl(vnptEkycProperties.getFacematchUrl())
                    .build();

            Map<String, Object> reqBody = new HashMap<>();
            reqBody.put("img_front", cardHash);
            reqBody.put("img_face", faceHash);
            reqBody.put("client_session", "session_" + UUID.randomUUID().toString());
            reqBody.put("token", "token_" + UUID.randomUUID().toString());

            String responseBody = client.post()
                    .headers(headers -> applyVnptHeaders(headers, customToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(reqBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(25))
                    .block();

            log.info("VNPT eKYC Face Compare Response: {}", responseBody);
            return parseFaceMatchResponse(responseBody);
        } catch (WebClientResponseException e) {
            String responseErr = e.getResponseBodyAsString();
            log.error("VNPT eKYC Face Compare HTTP Error [{}]: {}", e.getStatusCode(), responseErr);
            String detailedMsg = parseErrorMessage(responseErr, e.getMessage());
            throw new AppException(ErrorCode.EKYC_VERIFICATION_FAILED, "Lỗi từ VNPT eKYC Face Compare (" + e.getStatusCode().value() + "): " + detailedMsg);
        } catch (AppException ae) {
            throw ae;
        } catch (Exception e) {
            log.error("VNPT eKYC Face Compare request error: {}", e.getMessage());
            throw new AppException(ErrorCode.EKYC_VERIFICATION_FAILED, "Không thể kết nối dịch vụ VNPT eKYC Face Compare: " + e.getMessage());
        }
    }

    /**
     * Tải file ảnh lên VNPT IDG File Service để lấy mã hash phục vụ các API AI
     */
    private String uploadImageToFileService(MultipartFile file, String customToken) {
        String uploadUrl = vnptEkycProperties.getAddFileUrl();
        if (uploadUrl == null || uploadUrl.trim().isEmpty()) {
            uploadUrl = "https://api.idg.vnpt.vn/file-service/v1/addFile";
        }
        log.info("Gửi ảnh [{}] (kích thước: {} bytes) lên VNPT File Service: {}",
                file.getOriginalFilename(), file.getSize(), uploadUrl);

        try {
            WebClient client = WebClient.builder()
                    .baseUrl(uploadUrl)
                    .build();

            MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
            ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename() != null ? file.getOriginalFilename() : "image.jpg";
                }
            };
            MediaType mediaType = MediaType.IMAGE_JPEG;
            if (file.getContentType() != null && file.getContentType().equalsIgnoreCase(MediaType.IMAGE_PNG_VALUE)) {
                mediaType = MediaType.IMAGE_PNG;
            }
            bodyBuilder.part("file", resource, mediaType);
            bodyBuilder.part("title", file.getOriginalFilename() != null ? file.getOriginalFilename() : "ekyc_img.jpg");
            bodyBuilder.part("description", "Uploaded for VNPT eKYC verification");

            String responseBody = client.post()
                    .headers(headers -> applyVnptHeaders(headers, customToken))
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(25))
                    .block();

            log.info("VNPT addFile response: {}", responseBody);
            JsonNode root = objectMapper.readTree(responseBody);
            if (root.has("object") && root.get("object").has("hash")) {
                return root.get("object").get("hash").asText();
            }
            if (root.has("hash")) {
                return root.get("hash").asText();
            }
            throw new AppException(ErrorCode.EKYC_VERIFICATION_FAILED, "VNPT File Service không trả về mã hash ảnh.");
        } catch (WebClientResponseException e) {
            String err = e.getResponseBodyAsString();
            log.error("VNPT File Service upload error [{}]: {}", e.getStatusCode(), err);
            throw new AppException(ErrorCode.EKYC_VERIFICATION_FAILED, "Lỗi tải ảnh lên VNPT (" + e.getStatusCode().value() + "): " + parseErrorMessage(err, e.getMessage()));
        } catch (AppException ae) {
            throw ae;
        } catch (Exception e) {
            log.error("VNPT File Service upload error: {}", e.getMessage());
            throw new AppException(ErrorCode.EKYC_VERIFICATION_FAILED, "Không thể tải ảnh lên VNPT File Service: " + e.getMessage());
        }
    }

    /**
     * Gắn các tiêu chuẩn Authentication Header của VNPT eKYC
     */
    private void applyVnptHeaders(HttpHeaders headers, String customToken) {
        String effectiveToken = (customToken != null && !customToken.trim().isEmpty())
                ? customToken.trim()
                : vnptEkycProperties.getAccessToken();

        if (effectiveToken != null && !effectiveToken.trim().isEmpty()) {
            if (effectiveToken.toLowerCase().startsWith("bearer ")) {
                headers.set(HttpHeaders.AUTHORIZATION, effectiveToken);
            } else {
                headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + effectiveToken);
            }
            headers.set("api-key", effectiveToken);
        }

        if (vnptEkycProperties.getTokenId() != null && !vnptEkycProperties.getTokenId().trim().isEmpty()) {
            headers.set("Token-id", vnptEkycProperties.getTokenId().trim());
        }

        if (vnptEkycProperties.getTokenKey() != null && !vnptEkycProperties.getTokenKey().trim().isEmpty()) {
            headers.set("Token-key", vnptEkycProperties.getTokenKey().trim());
        }
    }

    private VnptOcrResult parseOcrResponse(String responseJson) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);

            // Kiểm tra mã lỗi từ VNPT
            if (root.has("statusCode") && root.get("statusCode").asInt() != 200) {
                String errMsg = parseErrorMessage(responseJson, "Lỗi bóc tách CCCD từ VNPT eKYC");
                throw new AppException(ErrorCode.EKYC_VERIFICATION_FAILED, "VNPT eKYC OCR: " + errMsg);
            }
            if (root.has("message") && !root.get("message").asText().equals("IDG-00000000")
                    && !root.get("message").asText().equalsIgnoreCase("SUCCESS")
                    && !root.get("message").asText().equalsIgnoreCase("OK")) {
                String errMsg = parseErrorMessage(responseJson, root.get("message").asText());
                throw new AppException(ErrorCode.EKYC_VERIFICATION_FAILED, "VNPT eKYC OCR: " + errMsg);
            }
            if (root.has("status") && !root.get("status").asText().equalsIgnoreCase("SUCCESS")
                    && !root.get("status").asText().equals("200")
                    && !root.get("status").asText().equalsIgnoreCase("OK")) {
                String errMsg = parseErrorMessage(responseJson, "Trạng thái nhận diện không thành công");
                throw new AppException(ErrorCode.EKYC_VERIFICATION_FAILED, "VNPT eKYC OCR: " + errMsg);
            }

            JsonNode targetNode = root.has("object") ? root.get("object") : (root.has("data") ? root.get("data") : root);

            // Trong trường hợp data là mảng
            if (targetNode.isArray() && !targetNode.isEmpty()) {
                targetNode = targetNode.get(0);
            }

            String idCardNumber = extractField(targetNode, "id", "id_number", "idCard", "id_card", "citizen_id");
            String fullName = extractField(targetNode, "name", "full_name", "fullName");
            String dob = extractField(targetNode, "birth_day", "dob", "birthday", "birth_date");
            String sex = extractField(targetNode, "gender", "sex");
            String address = extractField(targetNode, "recent_location", "address", "residence", "permanent_address");
            String hometown = extractField(targetNode, "origin_location", "hometown", "home", "place_of_origin");
            String issueDate = extractField(targetNode, "issue_date", "issueDate");
            String issuePlace = extractField(targetNode, "issue_place", "issuePlace");
            String cardType = extractField(targetNode, "card_type", "type");

            if (idCardNumber.isEmpty()) {
                throw new AppException(ErrorCode.EKYC_VERIFICATION_FAILED,
                        "VNPT eKYC không bóc tách được số CCCD trên ảnh. Vui lòng chụp rõ nét, đủ ánh sáng và không bị lóa.");
            }

            return VnptOcrResult.builder()
                    .idCardNumber(idCardNumber)
                    .fullName(fullName)
                    .dob(dob)
                    .sex(sex)
                    .address(address)
                    .hometown(hometown)
                    .issueDate(issueDate)
                    .issuePlace(issuePlace)
                    .cardType(!cardType.isEmpty() ? cardType : "chip_front")
                    .rawJson(responseJson)
                    .build();
        } catch (AppException ae) {
            throw ae;
        } catch (Exception e) {
            log.error("Lỗi phân tích cú pháp kết quả OCR VNPT eKYC: {}", e.getMessage());
            throw new AppException(ErrorCode.EKYC_VERIFICATION_FAILED, "Không thể đọc dữ liệu CCCD từ VNPT eKYC: " + e.getMessage());
        }
    }

    private VnptFaceMatchResult parseFaceMatchResponse(String responseJson) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);

            if (root.has("statusCode") && root.get("statusCode").asInt() != 200) {
                String errMsg = parseErrorMessage(responseJson, "Lỗi so khớp khuôn mặt từ VNPT eKYC");
                throw new AppException(ErrorCode.EKYC_VERIFICATION_FAILED, "VNPT eKYC Face Compare: " + errMsg);
            }
            if (root.has("message") && !root.get("message").asText().equals("IDG-00000000")
                    && !root.get("message").asText().equalsIgnoreCase("SUCCESS")
                    && !root.get("message").asText().equalsIgnoreCase("OK")) {
                String errMsg = parseErrorMessage(responseJson, root.get("message").asText());
                throw new AppException(ErrorCode.EKYC_VERIFICATION_FAILED, "VNPT eKYC Face Compare: " + errMsg);
            }

            JsonNode targetNode = root.has("object") ? root.get("object") : (root.has("data") ? root.get("data") : root);

            double similarity = 0.0;
            if (targetNode.has("prob")) {
                similarity = targetNode.get("prob").asDouble(0.0);
            } else if (targetNode.has("similarity")) {
                similarity = targetNode.get("similarity").asDouble(0.0);
            } else if (targetNode.has("matching_score")) {
                similarity = targetNode.get("matching_score").asDouble(0.0);
            }

            // Quy đổi sang thang 100% nếu VNPT trả về 0.0 - 1.0
            if (similarity > 0.0 && similarity <= 1.0) {
                similarity = similarity * 100.0;
            }
            similarity = Math.round(similarity * 10.0) / 10.0;

            boolean isMatch = similarity > vnptEkycProperties.getConfidenceThreshold();
            if (targetNode.has("result")) {
                String resStr = targetNode.get("result").asText("");
                if ("MATCH".equalsIgnoreCase(resStr) || "true".equalsIgnoreCase(resStr)) {
                    isMatch = true;
                }
            }

            return VnptFaceMatchResult.builder()
                    .similarity(similarity)
                    .isMatch(isMatch)
                    .rawJson(responseJson)
                    .build();
        } catch (AppException ae) {
            throw ae;
        } catch (Exception e) {
            log.error("Lỗi phân tích cú pháp kết quả Face Compare VNPT: {}", e.getMessage());
            throw new AppException(ErrorCode.EKYC_VERIFICATION_FAILED, "Không thể đọc dữ liệu so khớp khuôn mặt từ VNPT eKYC: " + e.getMessage());
        }
    }

    private String extractField(JsonNode node, String... fieldNames) {
        for (String name : fieldNames) {
            if (node.has(name) && !node.get(name).isNull()) {
                String val = node.get(name).asText();
                if (val != null && !val.trim().isEmpty() && !"N/A".equalsIgnoreCase(val.trim())) {
                    return val.trim();
                }
            }
            JsonNode infoNode = node.path("info");
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
                StringBuilder sb = new StringBuilder();
                if (root.has("message") && !root.get("message").asText().isEmpty() && !root.get("message").asText().equals("IDG-00000000")) {
                    sb.append(root.get("message").asText());
                }
                if (root.has("errors") && root.get("errors").isArray() && !root.get("errors").isEmpty()) {
                    List<String> errList = new ArrayList<>();
                    root.get("errors").forEach(item -> errList.add(item.asText()));
                    if (sb.length() > 0) sb.append(": ");
                    sb.append(String.join(", ", errList));
                } else if (root.has("error") && !root.get("error").asText().isEmpty()) {
                    if (sb.length() > 0) sb.append(": ");
                    sb.append(root.get("error").asText());
                } else if (root.has("errorMessage") && !root.get("errorMessage").asText().isEmpty()) {
                    if (sb.length() > 0) sb.append(": ");
                    sb.append(root.get("errorMessage").asText());
                }

                if (sb.length() > 0) {
                    return sb.toString();
                }
            }
        } catch (Exception ignored) {}
        return defaultMsg;
    }

    /**
     * Mô phỏng bóc tách OCR theo chuẩn VNPT AI (cho môi trường đồ án khi mockEnabled = true)
     */
    private VnptOcrResult simulateVnptOcr(MultipartFile frontImage, MultipartFile backImage) {
        log.info("[VNPT eKYC Smart Sandbox] Processing OCR simulation for image size: {} bytes", frontImage.getSize());

        String mockCccdNumber = "079098123456";

        return VnptOcrResult.builder()
                .idCardNumber(mockCccdNumber)
                .fullName("NGUYỄN VĂN AN")
                .dob("15/08/1998")
                .sex("Nam")
                .address("Phường 25, Quận Bình Thạnh, TP. Hồ Chí Minh")
                .hometown("TP. Hồ Chí Minh")
                .issueDate("10/05/2021")
                .issuePlace("Cục Cảnh sát QLHC về TTXH")
                .cardType("chip_front")
                .rawJson("{\"simulation\": true, \"source\": \"VNPT AI eKYC Platform\", \"statusCode\": 200}")
                .build();
    }

    /**
     * Mô phỏng so khớp khuôn mặt theo chuẩn VNPT Face Compare (> 85% hoặc mô phỏng từ chối khi khác khuôn mặt)
     */
    private VnptFaceMatchResult simulateVnptFaceMatch(MultipartFile cardImage, MultipartFile selfieImage, boolean forceMismatch) {
        String filename = (selfieImage.getOriginalFilename() != null) ? selfieImage.getOriginalFilename().toLowerCase() : "";
        log.info("[VNPT eKYC Smart Sandbox] Face Matching simulation for selfie: [{}], forceMismatch: {}", filename, forceMismatch);

        if (forceMismatch || filename.contains("fail") || filename.contains("reject") || filename.contains("khac") || filename.contains("diff")) {
            double lowScore = 48.0 + random.nextDouble() * 22.0;
            double roundedLow = Math.round(lowScore * 10.0) / 10.0;
            return VnptFaceMatchResult.builder()
                    .similarity(roundedLow)
                    .isMatch(false)
                    .rawJson("{\"simulation\": true, \"source\": \"VNPT AI eKYC\", \"result\": \"MISMATCH\", \"prob\": " + (roundedLow / 100.0) + "}")
                    .build();
        }

        double highScore = 90.0 + random.nextDouble() * 7.5;
        double roundedScore = Math.round(highScore * 10.0) / 10.0;

        return VnptFaceMatchResult.builder()
                .similarity(roundedScore)
                .isMatch(true)
                .rawJson("{\"simulation\": true, \"source\": \"VNPT AI eKYC\", \"result\": \"MATCH\", \"prob\": " + (roundedScore / 100.0) + "}")
                .build();
    }
}
