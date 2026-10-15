import { api } from './api';
import type {
  SmartCaCertificateInfo,
  SmartCaConfirmRequest,
  SmartCaInitiateRequest,
  SmartCaInitiateResponse,
  SmartCaStatusResponse,
} from '../types/smartca';

export const smartcaService = {
  /**
   * Khởi tạo giao dịch ký số hợp đồng qua VNPT SmartCA
   */
  async initiate(contractId: string, request?: SmartCaInitiateRequest): Promise<SmartCaInitiateResponse> {
    const res = await api.post<SmartCaInitiateResponse>(`/contracts/${contractId}/smartca/initiate`, request || {});
    return res.data;
  },

  /**
   * Xác nhận ký số (qua mã PIN hoặc sandbox confirm)
   */
  async confirm(contractId: string, request: SmartCaConfirmRequest): Promise<SmartCaStatusResponse> {
    const res = await api.post<SmartCaStatusResponse>(`/contracts/${contractId}/smartca/confirm`, request);
    return res.data;
  },

  /**
   * Tra cứu trạng thái giao dịch ký số SmartCA
   */
  async getStatus(contractId: string, transactionId: string): Promise<SmartCaStatusResponse> {
    const res = await api.get<SmartCaStatusResponse>(`/contracts/${contractId}/smartca/status/${transactionId}`);
    return res.data;
  },

  /**
   * Lấy danh sách chứng thư số cá nhân
   */
  async getCertificates(): Promise<SmartCaCertificateInfo[]> {
    const res = await api.get<SmartCaCertificateInfo[]>('/smartca/certificates');
    return res.data;
  },
};
