import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { paymentService, type VnpayPayment } from '../services/paymentService';
import { CheckCircle2, XCircle, AlertTriangle, Clock, ArrowRight, ShieldCheck, RefreshCw } from 'lucide-react';

export default function VnpayReturnPage() {
  const [params] = useSearchParams();
  const reference = params.get('vnp_TxnRef');
  const [payment, setPayment] = useState<VnpayPayment | null>(null);
  const [error, setError] = useState('');
  const [waiting, setWaiting] = useState(true);
  const [refresh, setRefresh] = useState(0);
  const [isConfirmingSandbox, setIsConfirmingSandbox] = useState(false);

  useEffect(() => {
    let disposed = false;
    let timer: ReturnType<typeof setTimeout> | undefined;

    const verify = async () => {
      if (!reference || !/^[a-f0-9]{32}$/.test(reference)) {
        setError('Thiếu hoặc sai mã giao dịch VNPAY.');
        setWaiting(false);
        return;
      }

      // Thu thập toàn bộ tham số trả về từ VNPAY
      const fields: Record<string, string> = {};
      params.forEach((val, key) => {
        fields[key] = val;
      });

      try {
        let result: VnpayPayment | null = null;

        // Nếu vừa redirect từ VNPAY về có kèm query params (vnp_ResponseCode)
        if (fields.vnp_ResponseCode) {
          try {
            result = await paymentService.processReturn(fields);
          } catch (e: any) {
            console.warn('processReturn error, falling back to reconcile/status:', e);
          }
        }

        // Nếu chưa có kết quả (hoặc refresh lại trang), thử reconcile hoặc lấy status
        if (!result) {
          try {
            const verification = await paymentService.reconcile(reference);
            result = verification.payment;
          } catch {
            result = await paymentService.status(reference);
          }
        }

        if (disposed) return;
        setPayment(result);
        setError('');
        setWaiting(false);
      } catch (err: any) {
        if (disposed) return;
        setError(err instanceof Error ? err.message : 'Không thể kiểm tra giao dịch.');
        setWaiting(false);
      }
    };

    void verify();
    return () => {
      disposed = true;
      clearTimeout(timer);
    };
  }, [reference, refresh, params]);

  const handleSandboxConfirm = async () => {
    if (!reference) return;
    setIsConfirmingSandbox(true);
    try {
      const confirmed = await paymentService.confirmSandbox(reference);
      setPayment(confirmed);
      setError('');
    } catch (err: any) {
      setError(err instanceof Error ? err.message : 'Không thể xác nhận giao dịch sandbox.');
    } finally {
      setIsConfirmingSandbox(false);
    }
  };

  const isSuccess = payment?.status === 'SUCCESS';
  const isFailed = payment?.status === 'FAILED';
  const isPending = waiting || payment?.status === 'PENDING';
  const isReviewRequired = payment?.status === 'REVIEW_REQUIRED';

  return (
    <main className="container" style={{ padding: '60px 20px', maxWidth: 680 }}>
      <div style={{
        background: '#fff',
        borderRadius: 16,
        padding: '40px 32px',
        boxShadow: '0 10px 30px rgba(0,0,0,0.06)',
        border: '1px solid #eef2f5',
        textAlign: 'center'
      }}>
        {/* Biểu tượng trạng thái */}
        {isSuccess && (
          <div style={{
            width: 72,
            height: 72,
            borderRadius: '50%',
            background: '#ecfdf5',
            color: '#10b981',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            margin: '0 auto 20px'
          }}>
            <CheckCircle2 size={44} strokeWidth={2.5} />
          </div>
        )}
        {isFailed && (
          <div style={{
            width: 72,
            height: 72,
            borderRadius: '50%',
            background: '#fef2f2',
            color: '#ef4444',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            margin: '0 auto 20px'
          }}>
            <XCircle size={44} strokeWidth={2.5} />
          </div>
        )}
        {isPending && (
          <div style={{
            width: 72,
            height: 72,
            borderRadius: '50%',
            background: '#eff6ff',
            color: '#3b82f6',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            margin: '0 auto 20px'
          }}>
            <Clock size={44} strokeWidth={2.5} />
          </div>
        )}
        {isReviewRequired && (
          <div style={{
            width: 72,
            height: 72,
            borderRadius: '50%',
            background: '#fffbeb',
            color: '#f59e0b',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            margin: '0 auto 20px'
          }}>
            <AlertTriangle size={44} strokeWidth={2.5} />
          </div>
        )}

        <h1 style={{ fontSize: 24, fontWeight: 700, margin: '0 0 12px', color: '#111827' }}>
          {isSuccess
            ? 'Thanh toán tiền cọc thành công!'
            : isFailed
            ? 'Thanh toán không thành công'
            : isPending
            ? 'Đang kiểm tra kết quả thanh toán'
            : 'Cần kiểm tra đối soát'}
        </h1>

        <p style={{ color: '#6b7280', fontSize: 15, lineHeight: 1.6, margin: '0 0 24px' }}>
          {error
            ? error
            : isSuccess
            ? 'Hệ thống đã ghi nhận tiền đặt cọc phòng trọ của bạn. Hợp đồng đã chuyển sang trạng thái sẵn sàng để hai bên tiến hành ký số VNPT SmartCA.'
            : isFailed
            ? 'Giao dịch thanh toán VNPAY đã bị hủy hoặc không thành công. Bạn có thể quay lại trang hợp đồng để thanh toán lại.'
            : isReviewRequired
            ? 'VNPAY báo đã thanh toán nhưng hợp đồng không còn đủ điều kiện nhận cọc. Vui lòng liên hệ quản trị viên để được hỗ trợ đối soát.'
            : waiting
            ? 'Đang kết nối với cổng thanh toán VNPAY để xác nhận kết quả…'
            : 'Chưa nhận được kết quả đối soát tự động từ cổng VNPAY Sandbox.'}
        </p>

        {/* Bảng chi tiết thanh toán */}
        {payment && (
          <div style={{
            background: '#f8fafc',
            borderRadius: 12,
            padding: '20px 24px',
            margin: '0 0 28px',
            textAlign: 'left',
            border: '1px solid #e2e8f0',
            fontSize: 14
          }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 10 }}>
              <span style={{ color: '#64748b' }}>Số tiền cọc:</span>
              <strong style={{ color: '#0f172a', fontSize: 16 }}>{payment.amount.toLocaleString('vi-VN')} đ</strong>
            </div>
            {reference && (
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 10 }}>
                <span style={{ color: '#64748b' }}>Mã giao dịch:</span>
                <span style={{ fontFamily: 'monospace', color: '#334155' }}>{reference}</span>
              </div>
            )}
            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
              <span style={{ color: '#64748b' }}>Trạng thái:</span>
              <span style={{
                fontWeight: 600,
                color: isSuccess ? '#16a34a' : isFailed ? '#dc2626' : isPending ? '#2563eb' : '#d97706'
              }}>
                {isSuccess
                  ? 'ĐÃ THANH TOÁN THÀNH CÔNG'
                  : isFailed
                  ? 'THẤT BẠI'
                  : isPending
                  ? 'CHỜ XÁC NHẬN'
                  : 'CẦN ĐỐI SOÁT'}
              </span>
            </div>
          </div>
        )}

        {/* Nút hành động */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12, alignItems: 'center' }}>
          {isSuccess && (
            <Link
              to="/contracts"
              className="btn btn-primary"
              style={{
                width: '100%',
                padding: '12px 24px',
                fontSize: 16,
                borderRadius: 10,
                display: 'flex',
                justifyContent: 'center',
                alignItems: 'center',
                gap: 8
              }}
            >
              <ShieldCheck size={20} />
              Tiến hành ký hợp đồng (VNPT SmartCA)
              <ArrowRight size={18} />
            </Link>
          )}

          {isPending && !waiting && (
            <div style={{ width: '100%', display: 'flex', flexDirection: 'column', gap: 10 }}>
              <button
                className="btn btn-outline"
                style={{ width: '100%', padding: '10px 20px', borderRadius: 8 }}
                onClick={() => { setWaiting(true); setRefresh(v => v + 1); }}
              >
                <RefreshCw size={16} /> Kiểm tra lại
              </button>

              <button
                className="btn"
                style={{
                  width: '100%',
                  padding: '12px 20px',
                  borderRadius: 8,
                  backgroundColor: '#f59e0b',
                  color: '#ffffff',
                  fontWeight: 600
                }}
                disabled={isConfirmingSandbox}
                onClick={handleSandboxConfirm}
              >
                {isConfirmingSandbox ? 'Đang xác nhận…' : '⚡ Xác nhận thanh toán thành công (Thử nghiệm Sandbox)'}
              </button>
            </div>
          )}

          {isFailed && (
            <Link
              to="/contracts"
              className="btn btn-outline"
              style={{ width: '100%', padding: '10px 20px', borderRadius: 8 }}
            >
              Quay lại danh sách hợp đồng
            </Link>
          )}

          {isSuccess && (
            <Link to="/contracts" style={{ color: '#64748b', fontSize: 14, textDecoration: 'underline' }}>
              Về danh sách hợp đồng
            </Link>
          )}
        </div>

        <div style={{ marginTop: 24, paddingTop: 16, borderTop: '1px solid #f1f5f9', fontSize: 13, color: '#94a3b8' }}>
          Môi trường thử nghiệm VNPAY Sandbox • Không trừ tiền thật
        </div>
      </div>
    </main>
  );
}
