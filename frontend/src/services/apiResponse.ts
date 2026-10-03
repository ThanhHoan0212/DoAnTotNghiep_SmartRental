import type { ApiResponse } from '../types/auth';

export class ApiError extends Error {
  public status: number;
  public data?: unknown;

  constructor(message: string, status: number, data?: unknown) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.data = data;
  }
}

export async function parseApiResponse<T>(response: Response): Promise<ApiResponse<T>> {
  const text = await response.text();
  let json: ApiResponse<T>;
  try {
    json = JSON.parse(text);
  } catch {
    const message = text.trim() === 'Invalid CORS request'
      ? 'Máy chủ từ chối địa chỉ truy cập hiện tại (CORS). Vui lòng kiểm tra địa chỉ frontend và cấu hình kết nối.'
      : `Máy chủ trả về phản hồi không hợp lệ (HTTP ${response.status}). Vui lòng thử lại sau.`;
    throw new ApiError(message, response.status);
  }

  if (!json || typeof json !== 'object' || typeof json.success !== 'boolean') {
    throw new ApiError('Phản hồi máy chủ không đúng định dạng.', response.status);
  }
  if (!response.ok || !json.success) {
    throw new ApiError(json.message || 'Đã xảy ra lỗi khi kết nối máy chủ', response.status, json.data);
  }
  return json;
}
