import { api } from './api';
import type {
  Amenity,
  CreateRoomRequest,
  RoomDetail,
  RoomSummary,
  UpdateRoomRequest,
} from '../types/room';

export interface PageResponse<T> {
  content: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  isLast: boolean;
}

export const roomService = {
  /**
   * Lấy danh sách phòng trọ đã duyệt kèm lọc & phân trang
   */
  async getApprovedRooms(params: {
    page?: number;
    size?: number;
    district?: string;
    minPrice?: number;
    maxPrice?: number;
    amenityId?: number;
  } = {}): Promise<PageResponse<RoomSummary>> {
    const searchParams = new URLSearchParams();
    if (params.page !== undefined) searchParams.append('page', params.page.toString());
    if (params.size !== undefined) searchParams.append('size', params.size.toString());
    if (params.district) searchParams.append('district', params.district);
    if (params.minPrice) searchParams.append('minPrice', params.minPrice.toString());
    if (params.maxPrice) searchParams.append('maxPrice', params.maxPrice.toString());
    if (params.amenityId) searchParams.append('amenityId', params.amenityId.toString());

    const queryString = searchParams.toString();
    const endpoint = queryString ? `/rooms?${queryString}` : '/rooms';
    const res = await api.get<PageResponse<RoomSummary>>(endpoint);
    return res.data;
  },

  /**
   * Xem chi tiết phòng trọ (tự động tăng view_count)
   */
  async getRoomDetail(id: string): Promise<RoomDetail> {
    const res = await api.get<RoomDetail>(`/rooms/${id}`);
    return res.data;
  },

  /**
   * Đăng tin phòng trọ mới (dành cho Chủ nhà & Admin)
   */
  async createRoom(data: CreateRoomRequest): Promise<RoomDetail> {
    const res = await api.post<RoomDetail>('/rooms', data);
    return res.data;
  },

  /**
   * Cập nhật thông tin tin đăng
   */
  async updateRoom(id: string, data: UpdateRoomRequest): Promise<RoomDetail> {
    const res = await api.put<RoomDetail>(`/rooms/${id}`, data);
    return res.data;
  },

  /**
   * Xóa tin đăng
   */
  async deleteRoom(id: string): Promise<void> {
    await api.delete<void>(`/rooms/${id}`);
  },

  /**
   * Lấy danh sách tin đăng của chủ nhà đang đăng nhập
   */
  async getMyRooms(params: {
    page?: number;
    size?: number;
    status?: string;
  } = {}): Promise<PageResponse<RoomSummary>> {
    const searchParams = new URLSearchParams();
    if (params.page !== undefined) searchParams.append('page', params.page.toString());
    if (params.size !== undefined) searchParams.append('size', params.size.toString());
    if (params.status) searchParams.append('status', params.status);

    const queryString = searchParams.toString();
    const endpoint = queryString ? `/rooms/my-rooms?${queryString}` : '/rooms/my-rooms';
    const res = await api.get<PageResponse<RoomSummary>>(endpoint);
    return res.data;
  },

  /**
   * Lấy danh mục tất cả các tiện ích
   */
  async getAllAmenities(): Promise<Amenity[]> {
    const res = await api.get<Amenity[]>('/amenities');
    return res.data;
  },
};
