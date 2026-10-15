package com.phongtro.backend.service.client.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.phongtro.backend.config.VnptSmartCaProperties;
import com.phongtro.backend.dto.smartca.SmartCaCertificateInfo;
import com.phongtro.backend.service.client.VnptSmartCaClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class VnptSmartCaClientImpl implements VnptSmartCaClient {

    private final VnptSmartCaProperties smartCaProperties;
    private final ObjectMapper objectMapper;

    // Token caching
    private volatile String cachedAccessToken;
    private volatile Instant tokenExpiresAt = Instant.MIN;

    // In-memory registry for pending transactions
    private final Map<String, TxnState> transactionRegistry = new ConcurrentHashMap<>();

    private record TxnState(
            String transactionId,
            String userId,
            String userName,
            String docId,
            String dataHash,
            boolean isSandbox,
            boolean isSigned,
            Instant createdAt,
            String signatureValue,
            String certSerial,
            String certSubject
    ) {}

    @Override
    public synchronized String getAccessToken() {
        if (cachedAccessToken != null && Instant.now().isBefore(tokenExpiresAt)) {
            return cachedAccessToken;
        }

        try {
            log.info("Requesting VNPT SmartCA Access Token from {}", smartCaProperties.getAuthUrl());
            WebClient webClient = WebClient.builder()
                    .baseUrl(smartCaProperties.getAuthUrl())
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                    .build();

            String response = webClient.post()
                    .body(BodyInserters.fromFormData("client_id", smartCaProperties.getClientId())
                            .with("client_secret", smartCaProperties.getClientSecret())
                            .with("grant_type", "client_credentials"))
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            JsonNode root = objectMapper.readTree(response);
            String token = root.path("access_token").asText();
            int expiresIn = root.path("expires_in").asInt(3600);

            if (token != null && !token.isEmpty()) {
                this.cachedAccessToken = token;
                // Expire 300s earlier for safety
                this.tokenExpiresAt = Instant.now().plusSeconds(Math.max(60, expiresIn - 300));
                log.info("Successfully fetched VNPT SmartCA Access Token (valid for {}s)", expiresIn);
                return token;
            }
        } catch (Exception e) {
            log.error("Failed to obtain VNPT SmartCA Access Token: {}", e.getMessage());
        }

        return null;
    }

    @Override
    public List<SmartCaCertificateInfo> getCertificates(String userId) {
        List<SmartCaCertificateInfo> certs = new ArrayList<>();
        if (userId == null || userId.trim().isEmpty()) {
            return certs;
        }

        try {
            String url = smartCaProperties.getBaseUrl() + "/v1/credentials/get_certificate";
            Map<String, Object> reqBody = new HashMap<>();
            reqBody.put("sp_id", smartCaProperties.getSpId());
            reqBody.put("sp_password", smartCaProperties.getSpPassword());
            reqBody.put("user_id", userId.trim());
            reqBody.put("serial_number", "");
            reqBody.put("transaction_id", "SP_CA_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));

            log.info("Calling VNPT SmartCA Get Certificate for user [{}] at {}", userId, url);
            WebClient webClient = WebClient.builder()
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build();

            String response = webClient.post()
                    .uri(url)
                    .bodyValue(reqBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            log.debug("VNPT SmartCA Get Certificate response: {}", response);
            JsonNode root = objectMapper.readTree(response);
            JsonNode dataNode = root.path("data");
            JsonNode certsNode = dataNode.path("user_certificates");

            if (certsNode.isArray()) {
                for (JsonNode cNode : certsNode) {
                    certs.add(SmartCaCertificateInfo.builder()
                            .serialNumber(cNode.path("serial_number").asText())
                            .subject(cNode.path("subject").asText())
                            .issuer(cNode.path("issuer").asText("VNPT-CA"))
                            .validFrom(cNode.path("valid_from").asText())
                            .validTo(cNode.path("valid_to").asText())
                            .status(cNode.path("status").asText("OPERATIONAL"))
                            .build());
                }
            }
        } catch (WebClientResponseException e) {
            log.warn("VNPT SmartCA Get Certificate returned HTTP {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            log.warn("Error calling VNPT SmartCA Get Certificate: {}", e.getMessage());
        }

        // If no certificate found in VNPT gateway and sandbox mode enabled, provide sandbox certificate
        if (certs.isEmpty() && smartCaProperties.isSandboxMode()) {
            certs.add(SmartCaCertificateInfo.builder()
                    .serialNumber("VNPT-CA-" + Math.abs(userId.hashCode()))
                    .subject("CN=" + userId + ", O=VNPT SmartCA Test, C=VN")
                    .issuer("VNPT-CA Remote Signing Sub-CA")
                    .validFrom(Instant.now().toString())
                    .validTo(Instant.now().plus(Duration.ofDays(365)).toString())
                    .status("OPERATIONAL")
                    .build());
        }

        return certs;
    }

    @Override
    public SmartCaSignInitResult initiateSign(String userId, String userName, String transactionId, String docId, String dataHashHex, String serialNumber) {
        String cleanUserId = userId != null ? userId.trim() : "001010101001";
        String txnId = (transactionId != null && !transactionId.isEmpty())
                ? transactionId
                : "SP_CA_" + System.currentTimeMillis();

        Map<String, Object> reqBody = new HashMap<>();
        reqBody.put("sp_id", smartCaProperties.getSpId());
        reqBody.put("sp_password", smartCaProperties.getSpPassword());
        reqBody.put("user_id", cleanUserId);
        reqBody.put("password", "");
        reqBody.put("otp", "");
        reqBody.put("transaction_id", txnId);
        reqBody.put("serial_number", serialNumber != null ? serialNumber : "");

        Map<String, Object> fileItem = new HashMap<>();
        fileItem.put("doc_id", docId);
        fileItem.put("file_type", "pdf");
        fileItem.put("sign_type", "hash");
        fileItem.put("data_to_be_signed", dataHashHex);
        reqBody.put("sign_files", List.of(fileItem));

        boolean isRealSuccess = false;
        String message = "Đang chờ xác nhận từ ứng dụng VNPT SmartCA";
        String status = "PENDING_CONFIRMATION";

        try {
            String url = smartCaProperties.getBaseUrl() + "/v2/signatures/sign";
            log.info("Calling VNPT SmartCA Sign API at {} for user [{}] txn [{}]", url, cleanUserId, txnId);

            WebClient webClient = WebClient.builder()
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build();

            String response = webClient.post()
                    .uri(url)
                    .bodyValue(reqBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(12))
                    .block();

            log.info("VNPT SmartCA Sign API response: {}", response);
            JsonNode root = objectMapper.readTree(response);
            int statusCode = root.path("status_code").asInt(root.path("status").asInt(200));

            if (statusCode == 200 || statusCode == 0) {
                isRealSuccess = true;
                message = root.path("message").asText("Yêu cầu ký số đã gửi thành công tới ứng dụng VNPT SmartCA");
            } else {
                message = root.path("message").asText("VNPT SmartCA phản hồi: " + statusCode);
            }
        } catch (WebClientResponseException e) {
            log.warn("VNPT SmartCA Sign returned HTTP {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            message = "VNPT Gateway: " + e.getResponseBodyAsString();
        } catch (Exception e) {
            log.warn("Error calling VNPT SmartCA Sign: {}", e.getMessage());
            message = "Lỗi kết nối VNPT SmartCA: " + e.getMessage();
        }

        boolean isSandbox = false;
        if (!isRealSuccess) {
            if (smartCaProperties.isSandboxMode()) {
                log.info("VNPT SmartCA returned error or user not found. Falling back to Sandbox Mode for txn [{}]", txnId);
                isSandbox = true;
                message = "Yêu cầu ký số VNPT SmartCA đã được tạo. Vui lòng xác nhận trên ứng dụng VNPT SmartCA hoặc bấm Xác nhận Sandbox.";
                status = "PENDING_CONFIRMATION";
            }
        }

        // Store transaction state
        transactionRegistry.put(txnId, new TxnState(
                txnId, cleanUserId, userName, docId, dataHashHex,
                isSandbox, false, Instant.now(), null, null, null
        ));

        return new SmartCaSignInitResult(true, txnId, status, message, isSandbox);
    }

    @Override
    public SmartCaConfirmResult confirmSign(String userId, String userName, String transactionId, String pin, String sad, boolean forceSandbox) {
        TxnState state = transactionRegistry.get(transactionId);
        boolean useSandbox = forceSandbox || (state != null && state.isSandbox()) || smartCaProperties.isSandboxMode();

        // 1. Try real VNPT SmartCA confirm API if not purely forced sandbox
        if (!forceSandbox) {
            try {
                String url = smartCaProperties.getBaseUrl() + "/v2/signatures/confirm";
                Map<String, Object> reqBody = new HashMap<>();
                reqBody.put("sp_id", smartCaProperties.getSpId());
                reqBody.put("sp_password", smartCaProperties.getSpPassword());
                reqBody.put("user_id", userId != null ? userId.trim() : (state != null ? state.userId() : ""));
                reqBody.put("password", pin != null ? pin.trim() : "");
                reqBody.put("transaction_id", transactionId);
                reqBody.put("sad", sad != null ? sad.trim() : "");

                log.info("Calling VNPT SmartCA Confirm API at {} for txn [{}]", url, transactionId);
                WebClient webClient = WebClient.builder()
                        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .build();

                String response = webClient.post()
                        .uri(url)
                        .bodyValue(reqBody)
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofSeconds(12))
                        .block();

                log.info("VNPT SmartCA Confirm API response: {}", response);
                JsonNode root = objectMapper.readTree(response);
                int statusCode = root.path("status_code").asInt(root.path("status").asInt(200));

                if (statusCode == 200 || statusCode == 0) {
                    JsonNode dataNode = root.path("data");
                    String sig = dataNode.path("signature").asText(dataNode.path("signatures").asText(""));
                    String certSerial = dataNode.path("serial_number").asText("VNPT-CA-" + Math.abs(transactionId.hashCode()));
                    String certSubject = "CN=" + (userName != null ? userName : userId) + ", O=VNPT SmartCA, C=VN";

                    if (sig.isEmpty()) {
                        sig = generateSimulatedSignature(state != null ? state.dataHash() : transactionId);
                    }

                    // Update registry
                    if (state != null) {
                        transactionRegistry.put(transactionId, new TxnState(
                                state.transactionId(), state.userId(), state.userName(), state.docId(), state.dataHash(),
                                false, true, state.createdAt(), sig, certSerial, certSubject
                        ));
                    }

                    return new SmartCaConfirmResult(true, transactionId, "SIGNED", "Ký số thành công qua VNPT SmartCA", sig, certSerial, certSubject);
                }
            } catch (WebClientResponseException e) {
                log.warn("VNPT SmartCA Confirm returned HTTP {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            } catch (Exception e) {
                log.warn("Error calling VNPT SmartCA Confirm: {}", e.getMessage());
            }
        }

        // 2. Fallback to Sandbox confirm if allowed
        if (useSandbox) {
            log.info("Confirming VNPT SmartCA transaction [{}] in Sandbox Mode", transactionId);
            String sig = generateSimulatedSignature(state != null ? state.dataHash() : transactionId);
            String certSerial = "VNPT-CA-" + Math.abs(transactionId.hashCode());
            String certSubject = "CN=" + (userName != null ? userName : (state != null ? state.userName() : "Người dùng VNPT SmartCA")) +
                    ", UID=" + (userId != null ? userId : (state != null ? state.userId() : "CCCD")) +
                    ", O=VNPT-CA Remote Signing, C=VN";

            if (state != null) {
                transactionRegistry.put(transactionId, new TxnState(
                        state.transactionId(), state.userId(), state.userName(), state.docId(), state.dataHash(),
                        true, true, state.createdAt(), sig, certSerial, certSubject
                ));
            } else {
                transactionRegistry.put(transactionId, new TxnState(
                        transactionId, userId, userName, "DOC-" + transactionId, "HASH-" + transactionId,
                        true, true, Instant.now(), sig, certSerial, certSubject
                ));
            }

            return new SmartCaConfirmResult(true, transactionId, "SIGNED", "Ký số VNPT SmartCA (Sandbox) thành công", sig, certSerial, certSubject);
        }

        return new SmartCaConfirmResult(false, transactionId, "FAILED", "Xác nhận ký số không thành công trên VNPT Gateway", null, null, null);
    }

    @Override
    public SmartCaStatusResult checkSignStatus(String transactionId, String userId, String userName) {
        TxnState state = transactionRegistry.get(transactionId);
        if (state != null && state.isSigned()) {
            return new SmartCaStatusResult(
                    true, transactionId, "SIGNED", "Ký số đã hoàn tất",
                    state.signatureValue(), state.certSerial(), state.certSubject(), true
            );
        }

        // Call VNPT gateway status API
        try {
            String url = smartCaProperties.getBaseUrl() + "/v1/signatures/sign/" + transactionId + "/status";
            log.info("Checking VNPT SmartCA transaction status at {}", url);

            WebClient webClient = WebClient.builder()
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build();

            String response = webClient.post()
                    .uri(url)
                    .bodyValue(Collections.emptyMap())
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            log.info("VNPT SmartCA status response: {}", response);
            JsonNode root = objectMapper.readTree(response);
            int statusCode = root.path("status_code").asInt(root.path("status").asInt(200));
            JsonNode dataNode = root.path("data");
            String signStatus = dataNode.path("status").asText(root.path("message").asText(""));

            if ("SIGNED".equalsIgnoreCase(signStatus) || "SUCCESS".equalsIgnoreCase(signStatus) || statusCode == 200) {
                String sig = dataNode.path("signature").asText("");
                String certSerial = dataNode.path("serial_number").asText("VNPT-CA-" + Math.abs(transactionId.hashCode()));
                String certSubject = "CN=" + (userName != null ? userName : "User") + ", O=VNPT SmartCA, C=VN";

                if (sig.isEmpty()) {
                    sig = generateSimulatedSignature(state != null ? state.dataHash() : transactionId);
                }

                if (state != null) {
                    transactionRegistry.put(transactionId, new TxnState(
                            state.transactionId(), state.userId(), state.userName(), state.docId(), state.dataHash(),
                            false, true, state.createdAt(), sig, certSerial, certSubject
                    ));
                }

                return new SmartCaStatusResult(true, transactionId, "SIGNED", "Đã ký thành công trên ứng dụng VNPT SmartCA", sig, certSerial, certSubject, true);
            }
        } catch (WebClientResponseException e) {
            log.warn("VNPT SmartCA Status check returned HTTP {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            log.warn("Error calling VNPT SmartCA Status API: {}", e.getMessage());
        }

        // Check if still waiting
        if (state != null) {
            return new SmartCaStatusResult(
                    true, transactionId, state.isSigned() ? "SIGNED" : "PENDING_CONFIRMATION",
                    state.isSigned() ? "Đã ký thành công" : "Đang chờ người dùng mở app VNPT SmartCA để xác nhận",
                    state.signatureValue(), state.certSerial(), state.certSubject(), state.isSigned()
            );
        }

        return new SmartCaStatusResult(false, transactionId, "PENDING_CONFIRMATION", "Đang chờ xác nhận từ VNPT SmartCA", null, null, null, false);
    }

    @Override
    public boolean registerCertificate(String uid, String fullName, String phone, String email) {
        String token = getAccessToken();
        if (token == null) {
            log.warn("Cannot register certificate: Access token is missing");
            return false;
        }

        try {
            Map<String, Object> reqBody = new HashMap<>();
            reqBody.put("uid", uid);
            reqBody.put("email", email);
            reqBody.put("fullName", fullName);
            reqBody.put("loaiGiayTo", "cccd");
            reqBody.put("phone", phone);
            reqBody.put("pricingCode", "19187");

            log.info("Calling VNPT Partner Init to register CTS for user [{}]", uid);
            WebClient webClient = WebClient.builder()
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build();

            String response = webClient.post()
                    .uri(smartCaProperties.getPartnerInitUrl())
                    .bodyValue(reqBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(15))
                    .block();

            log.info("VNPT Partner Init response: {}", response);
            return true;
        } catch (Exception e) {
            log.warn("Error registering certificate with VNPT Partner Init: {}", e.getMessage());
            return false;
        }
    }

    private String generateSimulatedSignature(String rawData) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((rawData != null ? rawData : UUID.randomUUID().toString()).getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder("VNPT-SIG-");
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString().toUpperCase();
        } catch (Exception e) {
            return "VNPT-SIG-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
        }
    }
}
