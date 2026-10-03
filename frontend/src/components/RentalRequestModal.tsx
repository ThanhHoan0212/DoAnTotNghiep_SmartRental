import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Calendar, DollarSign, FileText, AlertCircle, CheckCircle2, X, ShieldAlert } from 'lucide-react';
import type { RoomDetail } from '../types/room';
import type { Contract } from '../types/contract';
import { contractService } from '../services/contractService';
import { useAuth } from '../hooks/useAuth';

interface RentalRequestModalProps {
  room: RoomDetail;
  isOpen: boolean;
  onClose: () => void;
  onSuccess: (contract: Contract) => void;
}

export const RentalRequestModal: React.FC<RentalRequestModalProps> = ({
  room,
  isOpen,
  onClose,
  onSuccess,
}) => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const isVerified = Boolean(user?.isIdentityVerified || user?.role === 'ADMIN');

  // Tính ngày mặc định: bắt đầu từ ngày mai
  const getTomorrowString = () => {
    const d = new Date();
    d.setDate(d.getDate() + 1);
    return d.toISOString().split('T')[0];
  };

  const getFutureDateString = (monthsAhead: number) => {
    const d = new Date();
    d.setDate(d.getDate() + 1);
    d.setMonth(d.getMonth() + monthsAhead);
    return d.toISOString().split('T')[0];
  };

  const [startDate, setStartDate] = useState<string>(getTomorrowString());
  const [durationMonths, setDurationMonths] = useState<number>(6);
  const [endDate, setEndDate] = useState<string>(getFutureDateString(6));
  const [depositAmount, setDepositAmount] = useState<number>(room.price || 0);
  const [terms, setTerms] = useState<string>('');
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  if (!isOpen) return null;

  // Cập nhật ngày kết thúc khi thay đổi thời hạn
  const handleDurationChange = (months: number) => {
    setDurationMonths(months);
    const start = new Date(startDate);
    start.setMonth(start.getMonth() + months);
    setEndDate(start.toISOString().split('T')[0]);
  };

  // Cập nhật ngày bắt đầu
  const handleStartDateChange = (newDateStr: string) => {
    setStartDate(newDateStr);
    const start = new Date(newDateStr);
    start.setMonth(start.getMonth() + durationMonths);
    setEndDate(start.toISOString().split('T')[0]);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!isVerified) {
      setError('Bạn cần hoàn tất xác thực danh tính eKYC trước khi gửi yêu cầu thuê phòng.');
      return;
    }

    if (!startDate || !endDate) {
      setError('Vui lòng chọn ngày bắt đầu và kết thúc thuê.');
      return;
    }

    if (new Date(endDate) <= new Date(startDate)) {
      setError('Ngày kết thúc phải sau ngày bắt đầu thuê.');
      return;
    }

    if (depositAmount < 0) {
      setError('Tiền đặt cọc không thể âm.');
      return;
    }

    try {
      setIsSubmitting(true);
      const contract = await contractService.createContract({
        roomId: room.id,
        startDate,
        endDate,
        depositAmount,
        terms: terms.trim() || undefined,
      });
      onSuccess(contract);
    } catch (err: any) {
      console.error('Lỗi khi gửi yêu cầu thuê phòng:', err);
      const msg = err.response?.data?.message || err.message || 'Không thể tạo yêu cầu thuê phòng. Vui lòng thử lại sau.';
      setError(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  const primaryImage = room.images?.find((img) => img.isPrimary)?.imageUrl ||
    room.images?.[0]?.imageUrl ||
    'https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=600&q=80';

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal-container" onClick={(e) => e.stopPropagation()}>
        {/* Modal Header */}
        <div className="modal-header">
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <FileText size={20} color="#167c5a" />
            <h3>Gửi yêu cầu thuê phòng</h3>
          </div>
          <button type="button" className="modal-close-btn" onClick={onClose} aria-label="Đóng">
            <X size={20} />
          </button>
        </div>

        {/* Modal Body */}
        <form onSubmit={handleSubmit}>
          <div className="modal-body">
            {!isVerified && (
              <div
                style={{
                  display: 'flex',
                  flexDirection: 'column',
                  gap: '10px',
                  background: '#fffbeb',
                  border: '1px solid #fde68a',
                  color: '#92400e',
                  padding: '14px',
                  borderRadius: '10px',
                  marginBottom: '16px',
                  fontSize: '13px',
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontWeight: 700, color: '#b45309' }}>
                  <ShieldAlert size={18} style={{ flexShrink: 0 }} />
                  <span>Yêu cầu xác thực danh tính điện tử eKYC</span>
                </div>
                <div style={{ lineHeight: 1.5, color: '#78350f' }}>
                  Vui lòng xác thực danh tính trước khi gửi yêu cầu. Sau khi chủ phòng chấp nhận, bạn mới có thể thanh toán cọc và ký hợp đồng bằng các thao tác giả lập.
                </div>
                <button
                  type="button"
                  onClick={() => {
                    onClose();
                    navigate('/ekyc');
                  }}
                  style={{
                    alignSelf: 'flex-start',
                    background: '#d97706',
                    color: '#ffffff',
                    border: 'none',
                    borderRadius: '6px',
                    padding: '6px 14px',
                    fontSize: '12px',
                    fontWeight: 600,
                    cursor: 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '6px',
                  }}
                >
                  <span>🛡️ Xác thực eKYC ngay</span>
                  <span>→</span>
                </button>
              </div>
            )}

            {error && (
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '8px',
                  background: '#fef2f2',
                  border: '1px solid #fee2e2',
                  color: '#b91c1c',
                  padding: '12px',
                  borderRadius: '8px',
                  marginBottom: '16px',
                  fontSize: '13px',
                }}
              >
                <AlertCircle size={18} style={{ flexShrink: 0 }} />
                <span>{error}</span>
              </div>
            )}

            {/* Room Info Summary */}
            <div className="modal-room-preview">
              <img src={primaryImage} alt={room.title} />
              <div style={{ flex: 1 }}>
                <div className="title">{room.title}</div>
                <div className="address">{room.address}, {room.district}, {room.city}</div>
                <div className="price">
                  {room.price?.toLocaleString('vi-VN')} đ / tháng
                </div>
              </div>
            </div>

            {/* Thời gian thuê */}
            <div style={{ marginBottom: '18px' }}>
              <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: '#334155', marginBottom: '8px' }}>
                <Calendar size={15} style={{ display: 'inline', marginRight: '6px', verticalAlign: 'middle' }} />
                Thời hạn dự kiến thuê:
              </label>

              <div style={{ display: 'flex', gap: '8px', marginBottom: '12px' }}>
                {[3, 6, 12].map((months) => (
                  <button
                    key={months}
                    type="button"
                    onClick={() => handleDurationChange(months)}
                    style={{
                      flex: 1,
                      padding: '8px 12px',
                      borderRadius: '8px',
                      fontSize: '13px',
                      fontWeight: 600,
                      border: durationMonths === months ? '2px solid #167c5a' : '1px solid #cbd5e1',
                      background: durationMonths === months ? '#ecfdf5' : '#ffffff',
                      color: durationMonths === months ? '#065f46' : '#475569',
                      cursor: 'pointer',
                      transition: 'all 0.15s',
                    }}
                  >
                    {months} tháng
                  </button>
                ))}
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <div>
                  <span style={{ fontSize: '12px', color: '#64748b', display: 'block', marginBottom: '4px' }}>
                    Ngày dọn vào (Bắt đầu):
                  </span>
                  <input
                    type="date"
                    value={startDate}
                    min={new Date().toISOString().split('T')[0]}
                    onChange={(e) => handleStartDateChange(e.target.value)}
                    required
                    style={{
                      width: '100%',
                      padding: '8px 12px',
                      border: '1px solid #cbd5e1',
                      borderRadius: '8px',
                      fontSize: '13px',
                    }}
                  />
                </div>

                <div>
                  <span style={{ fontSize: '12px', color: '#64748b', display: 'block', marginBottom: '4px' }}>
                    Ngày kết thúc dự kiến:
                  </span>
                  <input
                    type="date"
                    value={endDate}
                    min={startDate}
                    onChange={(e) => setEndDate(e.target.value)}
                    required
                    style={{
                      width: '100%',
                      padding: '8px 12px',
                      border: '1px solid #cbd5e1',
                      borderRadius: '8px',
                      fontSize: '13px',
                    }}
                  />
                </div>
              </div>
            </div>

            {/* Tiền đặt cọc đề xuất */}
            <div style={{ marginBottom: '18px' }}>
              <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: '#334155', marginBottom: '8px' }}>
                <DollarSign size={15} style={{ display: 'inline', marginRight: '6px', verticalAlign: 'middle' }} />
                Tiền đặt cọc đề xuất (VNĐ):
              </label>

              <div style={{ display: 'flex', gap: '8px', marginBottom: '8px' }}>
                {[
                  { label: '1 tháng (Chuẩn)', value: room.price },
                  { label: '2 tháng', value: room.price * 2 },
                  { label: '50% (Nửa tháng)', value: Math.round(room.price * 0.5) },
                ].map((opt, idx) => (
                  <button
                    key={idx}
                    type="button"
                    onClick={() => setDepositAmount(opt.value)}
                    style={{
                      flex: 1,
                      padding: '6px 8px',
                      borderRadius: '6px',
                      fontSize: '12px',
                      border: depositAmount === opt.value ? '1.5px solid #167c5a' : '1px solid #e2e8f0',
                      background: depositAmount === opt.value ? '#ecfdf5' : '#f8fafc',
                      color: depositAmount === opt.value ? '#065f46' : '#475569',
                      cursor: 'pointer',
                    }}
                  >
                    {opt.label}
                  </button>
                ))}
              </div>

              <input
                type="number"
                value={depositAmount}
                min={0}
                step={100000}
                onChange={(e) => setDepositAmount(Number(e.target.value))}
                required
                style={{
                  width: '100%',
                  padding: '9px 12px',
                  border: '1px solid #cbd5e1',
                  borderRadius: '8px',
                  fontSize: '14px',
                  fontWeight: 600,
                  color: '#167c5a',
                }}
              />
              <span style={{ fontSize: '12px', color: '#64748b', marginTop: '4px', display: 'block' }}>
                Bằng chữ: <strong>{depositAmount ? depositAmount.toLocaleString('vi-VN') : 0} đồng</strong>
              </span>
            </div>

            {/* Ghi chú / Điều khoản mong muốn */}
            <div style={{ marginBottom: '18px' }}>
              <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: '#334155', marginBottom: '6px' }}>
                Ghi chú / Yêu cầu gửi tới Chủ nhà:
              </label>
              <textarea
                value={terms}
                onChange={(e) => setTerms(e.target.value)}
                rows={3}
                placeholder="Ví dụ: Dự kiến 2 người ở, có 1 xe máy, dọn vào cuối tuần này..."
                style={{
                  width: '100%',
                  padding: '10px 12px',
                  border: '1px solid #cbd5e1',
                  borderRadius: '8px',
                  fontSize: '13px',
                  fontFamily: 'inherit',
                  resize: 'vertical',
                }}
              />
            </div>

            {/* Bảng tóm tắt tài chính */}
            <div
              style={{
                background: '#f8fafc',
                border: '1px solid #e2e8f0',
                borderRadius: '10px',
                padding: '12px 16px',
                fontSize: '13px',
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '6px', color: '#475569' }}>
                <span>Giá thuê phòng hàng tháng:</span>
                <strong style={{ color: '#0f172a' }}>{room.price?.toLocaleString('vi-VN')} đ/tháng</strong>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '6px', color: '#475569' }}>
                <span>Tiền cọc giữ phòng (hoàn lại khi hết HĐ):</span>
                <strong style={{ color: '#0f172a' }}>{depositAmount?.toLocaleString('vi-VN')} đ</strong>
              </div>
              <div
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  paddingTop: '6px',
                  borderTop: '1px dashed #cbd5e1',
                  fontWeight: 700,
                  color: '#167c5a',
                }}
              >
                <span>Tổng khoản tiền chuẩn bị đợt đầu:</span>
                <span>{(room.price + depositAmount).toLocaleString('vi-VN')} đ</span>
              </div>
            </div>
          </div>

          {/* Modal Footer */}
          <div className="modal-footer">
            <button
              type="button"
              className="btn btn-outline"
              onClick={onClose}
              disabled={isSubmitting}
            >
              Hủy bỏ
            </button>

            <button
              type="submit"
              className="btn btn-primary"
              disabled={isSubmitting || !isVerified}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '6px',
                opacity: !isVerified ? 0.6 : 1,
                cursor: !isVerified ? 'not-allowed' : 'pointer',
              }}
            >
              {isSubmitting ? (
                'Đang gửi yêu cầu...'
              ) : !isVerified ? (
                '🛡️ Cần xác thực eKYC'
              ) : (
                <>
                  <CheckCircle2 size={16} />
                  Gửi yêu cầu thuê phòng
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
