import type { User } from './auth';

export type RoomStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'HIDDEN' | 'RESERVED' | 'RENTED';

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

export interface RoomSearchRequest {
  keyword?: string;
  district?: string;
  ward?: string;
  city?: string;
  minPrice?: number;
  maxPrice?: number;
  minArea?: number;
  maxArea?: number;
  amenityIds?: number[];
  latitude?: number;
  longitude?: number;
  radiusKm?: number;
  sortBy?: 'createdAt' | 'price' | 'area' | 'viewCount';
  sortDirection?: 'asc' | 'desc';
  page?: number;
  size?: number;
}

export interface ParsedQueryResponse {
  originalQuery?: string;
  keyword?: string;
  district?: string;
  minPrice?: number;
  maxPrice?: number;
  minArea?: number;
  maxArea?: number;
  detectedAmenities?: string[];
  amenityIds?: number[];
  source?: 'AI_SERVICE' | 'RULE_BASED_FALLBACK' | string;
}

export interface NlpSearchResponse {
  parsedQuery: ParsedQueryResponse;
  results: {
    content: RoomSummary[];
    pageNumber: number;
    pageSize: number;
    totalElements: number;
    totalPages: number;
    isLast: boolean;
  };
}

export interface DistrictCountResponse {
  district: string;
  count: number;
}