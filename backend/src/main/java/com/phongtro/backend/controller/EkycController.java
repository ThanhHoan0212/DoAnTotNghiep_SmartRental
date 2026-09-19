package com.phongtro.backend.controller;

import com.phongtro.backend.common.ApiResponse;
import com.phongtro.backend.dto.response.EkycStatusResponse;
import com.phongtro.backend.dto.response.EkycVerificationResponse;
import com.phongtro.backend.entity.EkycVerification;
import com.phongtro.backend.service.EkycService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ekyc")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "10. eKYC Identity Verification", description = "Định danh điện tử eKYC qua FPT.AI (CCCD OCR & Face Matching > 85%)")
public class EkycController {

    private final EkycService ekycService;

    @PostMapping(value = "/verify", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Xác thực danh tính eKYC (Chụp CCCD 2 mặt & Chân dung selfie)",
            description = "Gửi ảnh CCCD mặt trước, mặt sau, ảnh selfie và thông tin khai báo để bóc tách thông tin và so khớp khuôn mặt qua FPT.AI (hoặc Smart Sandbox). Điểm tin cậy > 85% sẽ được cấp chứng nhận.")
    public ResponseEntity<ApiResponse<EkycVerificationResponse>> verifyIdentity(
            Authentication authentication,
            @RequestParam("frontImage") MultipartFile frontImage,
            @RequestParam("backImage") MultipartFile backImage,
            @RequestParam("selfieImage") MultipartFile selfieImage,
            @RequestParam(value = "declaredIdCardNumber", required = false) String declaredIdCardNumber,
            @RequestParam(value = "declaredFullName", required = false) String declaredFullName,
            @RequestParam(value = "declaredDob", required = false) String declaredDob,
            @RequestParam(value = "fptApiKey", required = false) String fptApiKey,
            @RequestParam(value = "simulatedMismatch", required = false, defaultValue = "false") boolean simulatedMismatch
    ) {
        EkycVerificationResponse response = ekycService.verifyIdentity(
                frontImage, backImage, selfieImage,
                declaredIdCardNumber, declaredFullName, declaredDob,
                fptApiKey, simulatedMismatch,
                authentication.getName()
        );
        return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response));
    }

    @GetMapping("/status")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Xem trạng thái định danh eKYC của tài khoản hiện tại")
    public ResponseEntity<ApiResponse<EkycStatusResponse>> getEkycStatus(Authentication authentication) {
        EkycStatusResponse status = ekycService.getEkycStatus(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(status));
    }

    @GetMapping("/history")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Xem lịch sử các lần xác thực eKYC của tài khoản")
    public ResponseEntity<ApiResponse<List<EkycVerification>>> getEkycHistory(Authentication authentication) {
        List<EkycVerification> history = ekycService.getEkycHistory(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(history));
    }
}
