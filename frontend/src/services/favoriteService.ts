import { api } from './api';
import type { FavoriteStatusResponse, ToggleFavoriteResponse } from '../types/favorite';
import type { RoomSummary } from '../types/room';
import type { PageResponse } from './roomService';

export const favoriteService = {
  /**
   * Lưu hoặc hủy lưu phòng trọ yêu thích (Toggle)
   */
  async toggleFavorite(roomId: string): Promise<ToggleFavoriteResponse> {
    const res = await api.post<ToggleFavoriteResponse>(`/favorites/${roomId}/toggle`);
    return res.data;
  },

  /**
   * Kiểm tra trạng thái yêu thích của 1 phòng (bao gồm tổng số lượt lưu)
   */
  async checkFavoriteStatus(roomId: string): Promise<FavoriteStatusResponse> {
    const res = await api.get<FavoriteStatusResponse>(`/favorites/${roomId}/status`);
    return res.data;
  },

  /**
   * Lấy danh sách các phòng trọ tôi đã lưu yêu thích (phân trang)
   */
  async getMyFavoriteRooms(page = 0, size = 12): Promise<PageResponse<RoomSummary>> {
    const res = await api.get<PageResponse<RoomSummary>>(`/favorites/my-favorites?page=${page}&size=${size}`);
    return res.data;
  },

  /**
   * Lấy danh sách ID các phòng trọ tôi đã lưu yêu thích
   */
  async getMyFavoriteRoomIds(): Promise<string[]> {
    try {
      const res = await api.get<string[]>('/favorites/my-favorite-ids');
      return res.data || [];
    } catch {
      return [];
    }
  },

  /**
   * Xóa phòng khỏi danh sách yêu thích
   */
  async removeFavorite(roomId: string): Promise<void> {
    await api.delete<void>(`/favorites/${roomId}`);
  },
};
