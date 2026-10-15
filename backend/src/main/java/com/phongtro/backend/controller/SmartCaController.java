package com.phongtro.backend.controller;

import com.phongtro.backend.common.ApiResponse;
import com.phongtro.backend.dto.smartca.*;
import com.phongtro.backend.service.SmartCaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "VNPT SmartCA Remote Signing", description = "Tích hợp ký số từ xa hợp đồng thuê phòng qua VNPT SmartCA")
public class SmartCaController {

    private final SmartCaService smartCaService;

    @Operation(summary = "Khởi tạo giao dịch ký số hợp đồng qua VNPT SmartCA")
    @PostMapping("/api/v1/contracts/{id}/smartca/initiate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SmartCaInitiateResponse>> initiateSigning(
            @PathVariable UUID id,
            @RequestBody(required = false) SmartCaInitiateRequest request,
            Authentication authentication) {

        log.info("API initiate SmartCA sign for contract [{}] by user [{}]", id, authentication.getName());
        SmartCaInitiateResponse response = smartCaService.initiateSigning(
                id,
                authentication.getName(),
                request != null ? request : new SmartCaInitiateRequest()
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Xác nhận ký số qua VNPT SmartCA (bằng PIN hoặc Sandbox xác nhận)")
    @PostMapping("/api/v1/contracts/{id}/smartca/confirm")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SmartCaStatusResponse>> confirmSigning(
            @PathVariable UUID id,
            @Valid @RequestBody SmartCaConfirmRequest request,
            Authentication authentication) {

        log.info("API confirm SmartCA sign for contract [{}] txn [{}] by user [{}]",
                id, request.getTransactionId(), authentication.getName());
        SmartCaStatusResponse response = smartCaService.confirmSigning(
                id,
                authentication.getName(),
                request
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Tra cứu tiến trình ký số VNPT SmartCA của hợp đồng")
    @GetMapping("/api/v1/contracts/{id}/smartca/status/{transactionId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SmartCaStatusResponse>> getSigningStatus(
            @PathVariable UUID id,
            @PathVariable String transactionId,
            Authentication authentication) {

        log.info("API check SmartCA sign status for contract [{}] txn [{}]", id, transactionId);
        SmartCaStatusResponse response = smartCaService.getSigningStatus(
                id,
                authentication.getName(),
                transactionId
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Lấy danh sách chứng thư số cá nhân trên hệ thống VNPT SmartCA")
    @GetMapping("/api/v1/smartca/certificates")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<SmartCaCertificateInfo>>> getCertificates(Authentication authentication) {
        log.info("API get SmartCA certificates for user [{}]", authentication.getName());
        List<SmartCaCertificateInfo> certs = smartCaService.getCertificates(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(certs));
    }
}
