export type ContractStatus = 
  | 'PENDING' 
  | 'ACTIVE' 
  | 'REJECTED' 
  | 'EXPIRED' 
  | 'TERMINATED' 
  | 'CANCELLED';

export interface ContractRoomSummary {
  id: string;
  title: string;
  address: string;
  district: string;
  city?: string;
  price?: number;
  primaryImageUrl?: string;
}

export interface ContractUserSummary {
  id: string;
  fullName: string;
  email: string;
  phoneNumber?: string;
  avatarUrl?: string;
}

export interface Contract {
  id: string;
  contractCode?: string;
  contractNumber?: string;

  // Room fields (from backend ContractResponse)
  roomId?: string;
  roomTitle?: string;
  roomAddress?: string;
  roomDistrict?: string;
  primaryImageUrl?: string;
  room?: ContractRoomSummary;

  // Tenant fields
  tenantId?: string;
  tenantName?: string;
  tenantEmail?: string;
  tenantPhone?: string;
  tenant?: ContractUserSummary;

  // Landlord fields
  landlordId?: string;
  landlordName?: string;
  landlordEmail?: string;
  landlordPhone?: string;
  landlord?: ContractUserSummary;

  // Duration & Finance
  startDate: string; // YYYY-MM-DD
  endDate: string;   // YYYY-MM-DD
  monthlyRent: number;
  depositAmount: number;
  terms?: string;
  status: ContractStatus;
  cancellationReason?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateContractRequest {
  roomId: string;
  startDate: string; // YYYY-MM-DD
  endDate: string;   // YYYY-MM-DD
  depositAmount: number;
  terms?: string;
}

export interface UpdateContractStatusRequest {
  status: ContractStatus;
  reason?: string;
}
