import type { User } from './auth';

export type RoomStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'HIDDEN';

export interface Amenity {
  id: number;
  name: string;
  icon?: string;
}

export interface RoomImage {
  id: string;
  imageUrl: string;
  isPrimary: boolean;
}

export interface RoomImageRequest {
  imageUrl: string;
  isPrimary?: boolean;
}

export interface RoomSummary {
  id: string;
  title: string;
  price: number;
  area: number;
  address: string;
  district: string;
  city: string;
  primaryImageUrl?: string;
  status: RoomStatus;
  viewCount: number;
  createdAt: string;
}

export interface RoomDetail {
  id: string;
  landlord: User;
  title: string;
  description?: string;
  price: number;
  area: number;
  address: string;
  ward?: string;
  district: string;
  city: string;
  latitude?: number;
  longitude?: number;
  status: RoomStatus;
  viewCount: number;
  images: RoomImage[];
  amenities: Amenity[];
  createdAt: string;
  updatedAt: string;
}

export type Room = RoomDetail;

export interface CreateRoomRequest {
  title: string;
  description?: string;
  price: number;
  area: number;
  address: string;
  ward?: string;
  district: string;
  city?: string;
  latitude?: number;
  longitude?: number;
  amenityIds?: number[];
  images?: RoomImageRequest[];
}

export interface UpdateRoomRequest extends Partial<CreateRoomRequest> {
  status?: RoomStatus;
}