import { api } from './api';
import type { ChangePasswordRequest, UpdateUserRequest, User, UserStatus } from '../types/auth';

export const userService = {
  /**
   * Lấy thông tin tài khoản người dùng hiện tại
   */
  async getMe(): Promise<User> {
    const res = await api.get<User>('/users/me');
    return res.data;
  },

  /**
   * Cập nhật thông tin cá nhân (họ tên, sđt, avatar)
   */
  async updateProfile(data: UpdateUserRequest): Promise<User> {
    const res = await api.put<User>('/users/me', data);
    // Cập nhật lại user trong localStorage
    localStorage.setItem('user', JSON.stringify(res.data));
    return res.data;
  },

  /**
   * Đổi mật khẩu
   */
  async changePassword(data: ChangePasswordRequest): Promise<void> {
    await api.put<void>('/users/me/change-password', data);
  },

  /**
   * [Admin] Lấy danh sách tất cả người dùng
   */
  async getAllUsers(page = 0, size = 10, role?: string, status?: string) {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
    });
    if (role) params.append('role', role);
    if (status) params.append('status', status);

    const res = await api.get<any>(`/users?${params.toString()}`);
    return res.data;
  },

  /**
   * [Admin] Cập nhật trạng thái người dùng (ACTIVE / BANNED)
   */
  async updateUserStatus(userId: string, status: UserStatus): Promise<User> {
    const res = await api.patch<User>(`/users/${userId}/status`, { status });
    return res.data;
  },
};
