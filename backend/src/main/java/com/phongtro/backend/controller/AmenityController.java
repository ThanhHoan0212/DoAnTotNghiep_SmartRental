package com.phongtro.backend.controller;

import com.phongtro.backend.common.ApiResponse;
import com.phongtro.backend.dto.request.AmenityRequest;
import com.phongtro.backend.dto.response.AmenityResponse;
import com.phongtro.backend.service.AmenityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/amenities")
@RequiredArgsConstructor
@Tag(name = "3. Amenity Management", description = "Quản lý danh mục tiện ích phòng trọ (Wifi, Điều hòa, Nóng lạnh, v.v.)")
public class AmenityController {

    private final AmenityService amenityService;

    @GetMapping
    @Operation(summary = "Lấy danh sách tất cả tiện ích", description = "API công khai trả về danh sách các tiện ích có sẵn trong hệ thống")
    public ResponseEntity<ApiResponse<List<AmenityResponse>>> getAllAmenities() {
        List<AmenityResponse> response = amenityService.getAllAmenities();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "[Admin] Tạo tiện ích phòng trọ mới", description = "Cho phép quản trị viên bổ sung tiện ích mới")
    public ResponseEntity<ApiResponse<AmenityResponse>> createAmenity(@Valid @RequestBody AmenityRequest request) {
        AmenityResponse response = amenityService.createAmenity(request);
        return new ResponseEntity<>(
                ApiResponse.success("Thêm tiện ích thành công", response),
                HttpStatus.CREATED
        );
    }
}
