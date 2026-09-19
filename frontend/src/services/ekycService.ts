import { api } from './api';
import type { EkycStatusResponse, EkycVerificationResponse, EkycPersonalInfo } from '../types/ekyc';

export const ekycService = {
  /**
   * Gửi 3 file ảnh (CCCD trước, CCCD sau, Selfie) kèm thông tin khai báo để xác thực eKYC
   */
  async verifyEkyc(
    frontImage: File,
    backImage: File,
    selfieImage: File,
    personalInfo?: EkycPersonalInfo,
    fptApiKey?: string,
    simulatedMismatch?: boolean
  ): Promise<EkycVerificationResponse> {
    const formData = new FormData();
    formData.append('frontImage', frontImage);
    formData.append('backImage', backImage);
    formData.append('selfieImage', selfieImage);

    if (personalInfo) {
      if (personalInfo.idCardNumber) formData.append('declaredIdCardNumber', personalInfo.idCardNumber.trim());
      if (personalInfo.fullName) formData.append('declaredFullName', personalInfo.fullName.trim());
      if (personalInfo.dob) formData.append('declaredDob', personalInfo.dob.trim());
    }

    if (fptApiKey && fptApiKey.trim()) {
      formData.append('fptApiKey', fptApiKey.trim());
    }

    if (simulatedMismatch) {
      formData.append('simulatedMismatch', 'true');
    }

    const res = await api.post<EkycVerificationResponse>('/ekyc/verify', formData);
    return res.data;
  },

  /**
   * Lấy trạng thái định danh eKYC hiện tại
   */
  async getEkycStatus(): Promise<EkycStatusResponse> {
    const res = await api.get<EkycStatusResponse>('/ekyc/status');
    return res.data;
  },
};
