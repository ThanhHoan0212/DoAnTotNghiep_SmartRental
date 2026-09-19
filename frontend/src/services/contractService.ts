import { api } from './api';
import type {
  Contract,
  ContractStatus,
  CreateContractRequest,
  UpdateContractStatusRequest,
} from '../types/contract';
import type { PageResponse } from './roomService';

export const contractService = {
  /**
   * Tạo yêu cầu thuê phòng / hợp đồng đặt cọc mới (Người thuê)
   */
  async createContract(request: CreateContractRequest): Promise<Contract> {
    const res = await api.post<Contract>('/contracts', request);
    return res.data;
  },

  /**
   * Lấy chi tiết một hợp đồng theo ID
   */
  async getContractDetail(id: string): Promise<Contract> {
    const res = await api.get<Contract>(`/contracts/${id}`);
    return res.data;
  },

  /**
   * Lấy danh sách hợp đồng của tôi với tư cách Người thuê (Tenant)
   */
  async getMyTenantContracts(page = 0, size = 10): Promise<PageResponse<Contract>> {
    const res = await api.get<PageResponse<Contract>>(`/contracts/tenant?page=${page}&size=${size}`);
    return res.data;
  },

  /**
   * Lấy danh sách hợp đồng cho thuê của Chủ nhà (Landlord)
   */
  async getMyLandlordContracts(page = 0, size = 10): Promise<PageResponse<Contract>> {
    const res = await api.get<PageResponse<Contract>>(`/contracts/landlord?page=${page}&size=${size}`);
    return res.data;
  },

  /**
   * Lấy toàn bộ danh sách hợp đồng dành cho Quản trị viên (Admin)
   */
  async getAllContractsAdmin(
    page = 0,
    size = 10,
    status?: ContractStatus
  ): Promise<PageResponse<Contract>> {
    const statusParam = status ? `&status=${status}` : '';
    const res = await api.get<PageResponse<Contract>>(
      `/contracts/admin/all?page=${page}&size=${size}${statusParam}`
    );
    return res.data;
  },

  /**
   * Cập nhật trạng thái hợp đồng (Duyệt, Từ chối, Hủy, Chấm dứt)
   */
  async updateContractStatus(
    id: string,
    request: UpdateContractStatusRequest
  ): Promise<Contract> {
    const res = await api.patch<Contract>(`/contracts/${id}/status`, request);
    return res.data;
  },
};
