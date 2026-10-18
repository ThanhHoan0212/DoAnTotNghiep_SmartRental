package com.phongtro.backend.controller;

import com.phongtro.backend.common.*;
import com.phongtro.backend.dto.AdminDtos.*;
import com.phongtro.backend.service.AdminOperationsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/complaints") @RequiredArgsConstructor
public class SupportController {
    private final AdminOperationsService operations;
    @PostMapping @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public ApiResponse<ComplaintView> create(@Valid @RequestBody CreateComplaint request, Authentication auth) {
        return ApiResponse.success("Đã gửi khiếu nại đến bộ phận hỗ trợ", operations.createComplaint(request, auth.getName()));
    }
    @GetMapping("/mine")
    public ApiResponse<PageResponse<ComplaintView>> mine(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size, @RequestParam(required = false) java.util.UUID contractId,
            @RequestParam(required = false) com.phongtro.backend.entity.SupportComplaint.Status status, Authentication auth) {
        return ApiResponse.success(operations.complaints(page, size, status, auth.getName(), contractId));
    }
}
