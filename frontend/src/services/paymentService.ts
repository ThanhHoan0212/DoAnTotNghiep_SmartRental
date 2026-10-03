import { api } from './api';

export interface VnpayPayment {
  reference: string;
  contractId: string;
  status: 'PENDING' | 'SUCCESS' | 'FAILED' | 'REVIEW_REQUIRED';
  amount: number;
  paymentUrl: string;
  expiresAt: string;
  nextQueryAt: string | null;
}

export const paymentService = {
  async reconcile(reference: string): Promise<{ payment: VnpayPayment; message: string }> {
    return (await api.post<{ payment: VnpayPayment; message: string }>(`/payments/vnpay/${encodeURIComponent(reference)}/reconcile`)).data;
  },
  async create(contractId: string): Promise<VnpayPayment> {
    return (await api.post<VnpayPayment>(`/payments/vnpay/contracts/${contractId}`)).data;
  },
  async status(reference: string): Promise<VnpayPayment> {
    return (await api.get<VnpayPayment>(`/payments/vnpay/${encodeURIComponent(reference)}`)).data;
  },
};
