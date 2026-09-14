import { api } from './api';
import type { SaveSearchHistoryRequest, SearchHistoryItem } from '../types/favorite';

export const searchHistoryService = {
  /**
   * Lưu lại tiêu chí tìm kiếm người dùng vừa thực hiện
   */
  async saveSearchHistory(request: SaveSearchHistoryRequest): Promise<void> {
    try {
      await api.post<void>('/search-history', request);
    } catch (error) {
      // Background tracking, không chặn luồng UI nếu thất bại
      console.warn('Không thể lưu lịch sử tìm kiếm:', error);
    }
  },

  /**
   * Lấy danh sách tối đa 10 tìm kiếm gần đây
   */
  async getRecentSearches(): Promise<SearchHistoryItem[]> {
    try {
      const res = await api.get<SearchHistoryItem[]>('/search-history/recent');
      return res.data || [];
    } catch {
      return [];
    }
  },

  /**
   * Xóa 1 mục tìm kiếm cụ thể
   */
  async deleteSearchHistoryItem(id: string): Promise<void> {
    await api.delete<void>(`/search-history/${id}`);
  },

  /**
   * Xóa toàn bộ lịch sử tìm kiếm của người dùng
   */
  async clearAllSearchHistory(): Promise<void> {
    await api.delete<void>('/search-history/all');
  },
};
