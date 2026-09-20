package com.phongtro.backend.controller;

import com.phongtro.backend.common.ApiResponse;
import com.phongtro.backend.common.AppConstants;
import com.phongtro.backend.common.PageResponse;
import com.phongtro.backend.dto.request.CreateContractRequest;
import com.phongtro.backend.dto.request.UpdateContractStatusRequest;
import com.phongtro.backend.dto.response.ContractResponse;
import com.phongtro.backend.entity.ContractStatus;
import com.phongtro.backend.service.ContractService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/contracts")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "9. Contract Management", description = "Quản lý Hợp đồng thuê phòng & Đặt thuê giữa Người thuê và Chủ nhà")
public class ContractController {

    private final ContractService contractService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Người thuê gửi yêu cầu thuê phòng trọ (Tạo hợp đồng PENDING)",
            description = "Người thuê chọn phòng và thời hạn thuê để gửi yêu cầu đặt cọc/thuê phòng cho chủ nhà.")
    public ResponseEntity<ApiResponse<ContractResponse>> createContractRequest(
            Authentication authentication,
            @Valid @RequestBody CreateContractRequest request) {
        ContractResponse response = contractService.createContractRequest(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đã gửi yêu cầu thuê phòng thành công. Vui lòng chờ chủ nhà phê duyệt.", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Xem chi tiết hợp đồng thuê phòng",
            description = "Chỉ người thuê, chủ nhà liên quan đến hợp đồng hoặc Quản trị viên mới có quyền xem.")
    public ResponseEntity<ApiResponse<ContractResponse>> getContractById(
            Authentication authentication,
            @PathVariable UUID id) {
        ContractResponse response = contractService.getContractById(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/tenant")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lấy danh sách yêu cầu thuê / hợp đồng của người thuê hiện tại",
            description = "Trả về danh sách hợp đồng của tài khoản đang đăng nhập kèm phân trang và lọc theo trạng thái.")
    public ResponseEntity<ApiResponse<PageResponse<ContractResponse>>> getTenantContracts(
            Authentication authentication,
            @Parameter(description = "Lọc theo trạng thái hợp đồng (PENDING, ACTIVE, REJECTED, TERMINATED, CANCELLED)")
            @RequestParam(required = false) ContractStatus status,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size) {
        PageResponse<ContractResponse> response = contractService.getTenantContracts(
                authentication.getName(), status, page, size
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/landlord")
    @PreAuthorize("hasAnyRole('LANDLORD', 'ADMIN')")
    @Operation(summary = "Lấy danh sách hợp đồng thuê do chủ nhà quản lý",
            description = "Trả về danh sách yêu cầu thuê và hợp đồng thuộc các phòng trọ của chủ nhà hiện tại.")
    public ResponseEntity<ApiResponse<PageResponse<ContractResponse>>> getLandlordContracts(
            Authentication authentication,
            @Parameter(description = "Lọc theo trạng thái hợp đồng")
            @RequestParam(required = false) ContractStatus status,
            @Parameter(description = "Lọc theo phòng trọ cụ thể")
            @RequestParam(required = false) UUID roomId,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size) {
        PageResponse<ContractResponse> response = contractService.getLandlordContracts(
                authentication.getName(), status, roomId, page, size
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lấy danh sách toàn bộ hợp đồng trong hệ thống (dành cho Admin)")
    public ResponseEntity<ApiResponse<PageResponse<ContractResponse>>> getAllContractsForAdmin(
            @RequestParam(required = false) ContractStatus status,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size) {
        PageResponse<ContractResponse> response = contractService.getAllContractsForAdmin(status, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/simulate-deposit")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Giả lập người thuê thanh toán cọc, không thu tiền thật")
    public ResponseEntity<ApiResponse<ContractResponse>> simulateDeposit(Authentication authentication, @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(contractService.simulateDeposit(id, authentication.getName())));
    }

    @PostMapping("/{id}/simulate-signature")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Giả lập chữ ký của tài khoản hiện tại, không phải chữ ký số thật")
    public ResponseEntity<ApiResponse<ContractResponse>> simulateSignature(Authentication authentication, @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(contractService.simulateSignature(id, authentication.getName())));
    }

    @PostMapping("/{id}/closure-requests")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ContractResponse>> requestClosure(Authentication authentication, @PathVariable UUID id,
            @Valid @RequestBody com.phongtro.backend.dto.request.CreateClosureRequest request) {
        return ResponseEntity.ok(ApiResponse.success(contractService.requestClosure(id, request, authentication.getName())));
    }

    @PostMapping("/{id}/closure-requests/{requestId}/response")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ContractResponse>> respondClosure(Authentication authentication, @PathVariable UUID id,
            @PathVariable UUID requestId, @Valid @RequestBody com.phongtro.backend.dto.request.RespondClosureRequest request) {
        return ResponseEntity.ok(ApiResponse.success(contractService.respondClosure(id, requestId, request, authentication.getName())));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cập nhật trạng thái hợp đồng (Duyệt, từ chối, hủy, thanh lý)",
            description = "Chủ nhà có thể chấp nhận và mở cọc (AWAITING_DEPOSIT) hoặc từ chối (REJECTED); Người thuê có thể hủy (CANCELLED); Chấm dứt trước hạn phải qua yêu cầu riêng được bên còn lại xác nhận.")
    public ResponseEntity<ApiResponse<ContractResponse>> updateContractStatus(
            Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateContractStatusRequest request) {
        ContractResponse response = contractService.updateContractStatus(id, request, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái hợp đồng thành công", response));
    }
}
