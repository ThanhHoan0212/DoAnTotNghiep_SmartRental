import { api } from './api';
import type {
  AuthResponse,
  ForgotPasswordRequest,
  LoginRequest,
  RefreshTokenRequest,
  RegisterRequest,
  ResetPasswordRequest,
  TokenRefreshResponse,
} from '../types/auth';

export const authService = {
  /**
   * Đăng ký tài khoản người dùng mới
   */
  async register(data: RegisterRequest): Promise<AuthResponse> {
    const res = await api.post<AuthResponse>('/auth/register', data);
    if (res.data.accessToken) {
      localStorage.setItem('accessToken', res.data.accessToken);
      localStorage.setItem('refreshToken', res.data.refreshToken);
      localStorage.setItem('user', JSON.stringify(res.data.user));
    }
    return res.data;
  },

  /**
   * Đăng nhập hệ thống
   */
  async login(data: LoginRequest): Promise<AuthResponse> {
    const res = await api.post<AuthResponse>('/auth/login', data);
    if (res.data.accessToken) {
      localStorage.setItem('accessToken', res.data.accessToken);
      localStorage.setItem('refreshToken', res.data.refreshToken);
      localStorage.setItem('user', JSON.stringify(res.data.user));
    }
    return res.data;
  },

  /**
   * Cấp mới Access Token bằng Refresh Token
   */
  async refreshToken(refreshToken: string): Promise<TokenRefreshResponse> {
    const res = await api.post<TokenRefreshResponse>('/auth/refresh-token', {
      refreshToken,
    } as RefreshTokenRequest);
    return res.data;
  },

  /**
   * Đăng xuất và thu hồi Refresh Token trên máy chủ
   */
  async logout(): Promise<void> {
    const refreshToken = localStorage.getItem('refreshToken');
    try {
      if (refreshToken) {
        await api.post('/auth/logout', { refreshToken });
      }
    } catch (err) {
      console.warn('Lỗi khi thu hồi refresh token trên máy chủ:', err);
    } finally {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('user');
      window.dispatchEvent(new Event('auth:logout'));
    }
  },

  /**
   * Yêu cầu gửi mã OTP khôi phục mật khẩu
   */
  async forgotPassword(data: ForgotPasswordRequest): Promise<void> {
    await api.post('/auth/forgot-password', data);
  },

  /**
   * Đặt lại mật khẩu mới bằng OTP
   */
  async resetPassword(data: ResetPasswordRequest): Promise<void> {
    await api.post('/auth/reset-password', data);
  },
};
