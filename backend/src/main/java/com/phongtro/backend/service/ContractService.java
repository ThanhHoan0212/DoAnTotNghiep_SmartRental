package com.phongtro.backend.service;

import com.phongtro.backend.common.PageResponse;
import com.phongtro.backend.dto.request.CreateContractRequest;
import com.phongtro.backend.dto.request.UpdateContractStatusRequest;
import com.phongtro.backend.dto.response.ContractResponse;
import com.phongtro.backend.entity.ContractStatus;

import java.util.UUID;

public interface ContractService {

    /**
     * Người thuê gửi yêu cầu thuê phòng trọ (Tạo hợp đồng ở trạng thái PENDING)
     */
    ContractResponse createContractRequest(CreateContractRequest request, String tenantEmail);

    /**
     * Xem chi tiết 1 hợp đồng (chỉ tenant, landlord liên quan hoặc Admin mới có quyền)
     */
    ContractResponse getContractById(UUID contractId, String userEmail);

    /**
     * Danh sách hợp đồng của Người thuê đang đăng nhập
     */
    PageResponse<ContractResponse> getTenantContracts(String tenantEmail, ContractStatus status, int page, int size);

    /**
     * Danh sách hợp đồng thuộc các phòng trọ do Chủ nhà quản lý
     */
    PageResponse<ContractResponse> getLandlordContracts(String landlordEmail, ContractStatus status, UUID roomId, int page, int size);

    /**
     * Danh sách tất cả hợp đồng trong hệ thống (dành cho Admin)
     */
    PageResponse<ContractResponse> getAllContractsForAdmin(ContractStatus status, int page, int size);

    /**
     * Cập nhật trạng thái hợp đồng:
     * - Chủ nhà: Duyệt (ACTIVE), Từ chối (REJECTED), hoặc Thanh lý trước hạn (TERMINATED)
     * - Người thuê: Hủy yêu cầu (CANCELLED) nếu còn PENDING
     * - Admin: Toàn quyền cập nhật
     */
    ContractResponse updateContractStatus(UUID contractId, UpdateContractStatusRequest request, String userEmail);
}
