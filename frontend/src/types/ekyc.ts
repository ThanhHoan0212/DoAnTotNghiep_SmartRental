export interface EkycVerificationResponse {
  isSuccess: boolean;
  confidenceScore: number;
  status: 'SUCCESS' | 'FAILED';
  message: string;
  idCardNumberMasked?: string;
  idCardName?: string;
  idCardDob?: string;
  idCardAddress?: string;
  idCardHometown?: string;
  verifiedAt?: string;
}

export interface EkycStatusResponse {
  isIdentityVerified: boolean;
  idCardNumberMasked?: string;
  idCardName?: string;
  confidenceScore?: number;
  verifiedAt?: string;
}

export interface EkycPersonalInfo {
  fullName: string;
  idCardNumber: string;
  dob: string;
  gender: 'Nam' | 'Nữ' | 'Khác';
  hometown: string;
  address: string;
  phone: string;
}
