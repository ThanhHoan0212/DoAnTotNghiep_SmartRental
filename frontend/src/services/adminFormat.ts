export const labels: Record<string, string> = {
  ADMIN: 'Quản trị viên', TENANT: 'Người thuê', LANDLORD: 'Chủ nhà', ACTIVE: 'Đang hoạt động',
  INACTIVE: 'Chưa kích hoạt', BANNED: 'Đã khóa', PENDING: 'Chờ duyệt', APPROVED: 'Đã duyệt',
  REJECTED: 'Từ chối', HIDDEN: 'Đã ẩn', RESERVED: 'Đang giữ chỗ', RENTED: 'Đã cho thuê',
  OPEN: 'Mới tiếp nhận', IN_PROGRESS: 'Đang xử lý', RESOLVED: 'Đã giải quyết',
  FRAUD: 'Nghi ngờ lừa đảo', LISTING: 'Tin đăng',
  ACCOUNT: 'Tài khoản', PAYMENT: 'Thanh toán', OTHER: 'Khác', AWAITING_DEPOSIT: 'Chờ đặt cọc',
  AWAITING_SIGNATURES: 'Chờ ký', EXPIRED: 'Hết hạn', TERMINATED: 'Đã chấm dứt', CANCELLED: 'Đã hủy',
};
export const money = (value: number) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(value);
export const date = (value?: string) => value ? new Date(value).toLocaleString('vi-VN', { timeZone: 'Asia/Ho_Chi_Minh' }) : '—';
export const message = (error: unknown) => error instanceof Error ? error.message : 'Không thể thực hiện thao tác. Vui lòng thử lại.';
