import type { ApiResponse, TokenRefreshResponse } from '../types/auth';

const BASE_URL = import.meta.env.VITE_API_URL || '/api/v1';

export class ApiError extends Error {
  public status: number;
  public data?: any;

  constructor(message: string, status: number, data?: any) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.data = data;
  }
}

let isRefreshing = false;
let refreshSubscribers: ((token: string) => void)[] = [];

function subscribeTokenRefresh(cb: (token: string) => void) {
  refreshSubscribers.push(cb);
}

function onRefreshed(token: string) {
  refreshSubscribers.forEach((cb) => cb(token));
  refreshSubscribers = [];
}

async function refreshAccessToken(): Promise<string | null> {
  const refreshToken = localStorage.getItem('refreshToken');
  if (!refreshToken) {
    return null;
  }

  try {
    const res = await fetch(`${BASE_URL}/auth/refresh-token`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ refreshToken }),
    });

    if (!res.ok) {
      throw new Error('Refresh token expired');
    }

    const json: ApiResponse<TokenRefreshResponse> = await res.json();
    if (json.success && json.data.accessToken) {
      localStorage.setItem('accessToken', json.data.accessToken);
      if (json.data.refreshToken) {
        localStorage.setItem('refreshToken', json.data.refreshToken);
      }
      return json.data.accessToken;
    }
  } catch (error) {
    console.error('Failed to refresh access token:', error);
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
    window.dispatchEvent(new Event('auth:logout'));
  }
  return null;
}

export async function request<T>(
  endpoint: string,
  options: RequestInit = {}
): Promise<ApiResponse<T>> {
  const url = endpoint.startsWith('http') ? endpoint : `${BASE_URL}${endpoint}`;

  const headers = new Headers(options.headers || {});
  if (!headers.has('Content-Type') && !(options.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json');
  }

  const token = localStorage.getItem('accessToken');
  if (token && !headers.has('Authorization')) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  let response = await fetch(url, {
    ...options,
    headers,
  });

  // Xử lý tự động Refresh Token khi gặp lỗi 401
  if (response.status === 401 && !endpoint.includes('/auth/login') && !endpoint.includes('/auth/refresh-token')) {
    if (!isRefreshing) {
      isRefreshing = true;
      const newToken = await refreshAccessToken();
      isRefreshing = false;

      if (newToken) {
        onRefreshed(newToken);
        headers.set('Authorization', `Bearer ${newToken}`);
        return fetch(url, { ...options, headers }).then((res) => res.json());
      }
    } else {
      // Đợi refresh token hoàn tất rồi retry
      return new Promise((resolve) => {
        subscribeTokenRefresh(async (newToken: string) => {
          headers.set('Authorization', `Bearer ${newToken}`);
          const retryRes = await fetch(url, { ...options, headers });
          resolve(retryRes.json());
        });
      });
    }
  }

  const json: ApiResponse<T> = await response.json();

  if (!response.ok || !json.success) {
    const errorMsg = json.message || 'Đã xảy ra lỗi khi kết nối máy chủ';
    throw new ApiError(errorMsg, response.status, json.data);
  }

  return json;
}

export const api = {
  get: <T>(endpoint: string) => request<T>(endpoint, { method: 'GET' }),
  post: <T>(endpoint: string, body?: any) =>
    request<T>(endpoint, {
      method: 'POST',
      body: body ? JSON.stringify(body) : undefined,
    }),
  put: <T>(endpoint: string, body?: any) =>
    request<T>(endpoint, {
      method: 'PUT',
      body: body ? JSON.stringify(body) : undefined,
    }),
  patch: <T>(endpoint: string, body?: any) =>
    request<T>(endpoint, {
      method: 'PATCH',
      body: body ? JSON.stringify(body) : undefined,
    }),
  delete: <T>(endpoint: string) => request<T>(endpoint, { method: 'DELETE' }),
};
