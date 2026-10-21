import { api } from './api';
import type { RoomStatus } from '../types/room';
export type ComplaintStatus = 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'REJECTED';
export type ComplaintCategory = 'FRAUD' | 'LISTING' | 'ACCOUNT' | 'PAYMENT' | 'OTHER';
export interface Complaint {
  id: string; reporterId: string; reporterName: string; reporterEmail: string;
  roomId?: string; roomTitle?: string; contractId?: string; contractCode?: string; subject: string; category: ComplaintCategory;
  description: string; status: ComplaintStatus; response?: string; handledBy?: string;
  version: number; createdAt: string; updatedAt: string;
}
export interface Distribution { label: string; count: number; averageRent?: number }
export interface Analytics {
  from: string; to: string;
  snapshot: { totalUsers: number; activeUsers: number; totalRooms: number; pendingRooms: number;
    rentedRooms: number; activeContracts: number; openComplaints: number; averageRent: number };
  monthly: { month: string; users: number; rooms: number; contracts: number; depositCollected: number }[];
  roomStatuses: Distribution[]; userRoles: Distribution[]; districts: Distribution[]; contractStatuses: Distribution[];
}
export const adminService = {
  roomStatus: (id: string, status: RoomStatus) => api.patch(`/rooms/${id}/status`, { status }),
  handleComplaint: (item: Complaint, status: ComplaintStatus, response: string) =>
    api.patch(`/admin/complaints/${item.id}`, { status, response, version: item.version }),
};
