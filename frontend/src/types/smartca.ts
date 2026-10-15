import type { Contract } from './contract';

export interface SmartCaInitiateRequest {
  idCardNumber?: string;
  serialNumber?: string;
}

export interface SmartCaInitiateResponse {
  contractId: string;
  transactionId: string;
  docId: string;
  dataHash: string;
  signerCccd: string;
  signerName: string;
  signerRole: 'TENANT' | 'LANDLORD';
  status: string;
  message: string;
  sandbox: boolean;
}

export interface SmartCaConfirmRequest {
  transactionId: string;
  pin?: string;
  otp?: string;
  sandboxConfirm?: boolean;
}

export interface SmartCaStatusResponse {
  contractId: string;
  transactionId: string;
  status: 'PENDING' | 'SIGNED' | 'REJECTED' | 'FAILED' | 'PENDING_CONFIRMATION' | string;
  certSerial?: string;
  certSubject?: string;
  signedAt?: string;
  signatureValue?: string;
  contractCompleted: boolean;
  message?: string;
  contract?: Contract;
}

export interface SmartCaCertificateInfo {
  serialNumber: string;
  subject: string;
  issuer: string;
  validFrom: string;
  validTo: string;
  status: string;
}
