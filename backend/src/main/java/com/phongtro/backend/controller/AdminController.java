package com.phongtro.backend.controller;

import com.phongtro.backend.common.*;
import com.phongtro.backend.dto.AdminDtos.*;
import com.phongtro.backend.dto.response.UserResponse;
import com.phongtro.backend.entity.*;
import com.phongtro.backend.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.UUID;

@RestController @RequestMapping("/api/v1/admin")
@RequiredArgsConstructor @PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final AdminOperationsService operations;
    private final AdminAnalyticsService analytics;
    private final RoomService roomService;
    @GetMapping("/rooms/{id}")
    public ApiResponse<com.phongtro.backend.dto.response.RoomResponse> room(@PathVariable UUID id) {
        return ApiResponse.success(roomService.getRoomDetail(id, false));
    }
    @GetMapping("/users")
    public ApiResponse<PageResponse<UserResponse>> users(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size, @RequestParam(required = false) Role role,
            @RequestParam(required = false) UserStatus status, @RequestParam(required = false) String keyword) {
        return ApiResponse.success(operations.users(page, size, role, status, keyword));
    }
    @GetMapping("/complaints")
    public ApiResponse<PageResponse<ComplaintView>> complaints(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size, @RequestParam(required = false) SupportComplaint.Status status) {
        return ApiResponse.success(operations.complaints(page, size, status, null));
    }
    @PatchMapping("/complaints/{id}")
    public ApiResponse<ComplaintView> handle(@PathVariable UUID id, @Valid @RequestBody HandleComplaint request, Authentication auth) {
        return ApiResponse.success("Đã cập nhật phản hồi khiếu nại", operations.handleComplaint(id, request, auth.getName()));
    }
    @GetMapping("/analytics")
    public ApiResponse<AdminAnalyticsService.Report> report(@RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        return ApiResponse.success(analytics.report(from, to));
    }
}
