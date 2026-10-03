import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { paymentService, type VnpayPayment } from '../services/paymentService';

export default function VnpayReturnPage() {
  const [params] = useSearchParams();
  const reference = params.get('vnp_TxnRef');
  const [payment, setPayment] = useState<VnpayPayment | null>(null);
  const [error, setError] = useState('');
  const [waiting, setWaiting] = useState(true);
  const [refresh, setRefresh] = useState(0);
  const [verificationMessage, setVerificationMessage] = useState('');

  useEffect(() => {
    let disposed = false;
    let timer: ReturnType<typeof setTimeout>;
    let attempts = 0;
    const check = async () => {
      if (!reference || !/^[a-f0-9]{32}$/.test(reference)) {
        setError('Thiếu hoặc sai mã giao dịch VNPAY.');
        setWaiting(false);
        return;
      }
      try {
        const verification = attempts === 0 ? await paymentService.reconcile(reference) : null;
        const result = verification?.payment ?? await paymentService.status(reference);
        if (disposed) return;
        if (verification) setVerificationMessage(verification.message);
        setPayment(result);
        setError('');
        attempts++;
        if (result.status === 'PENDING' && attempts < 20) timer = setTimeout(check, 3000);
        else setWaiting(false);
      } catch (err) {
        if (disposed) return;
        setError(err instanceof Error ? err.message : 'Không thể kiểm tra giao dịch.');
        setWaiting(false);
      }
    };
    void check();
    return () => { disposed = true; clearTimeout(timer); };
  }, [reference, refresh]);

  const message = payment?.status === 'SUCCESS'
    ? 'Đã xác nhận thanh toán cọc. Hợp đồng đã sẵn sàng để ký.'
    : payment?.status === 'FAILED'
      ? 'Giao dịch không thành công hoặc đã bị hủy. Bạn có thể thử lại từ trang hợp đồng.'
      : payment?.status === 'REVIEW_REQUIRED'
        ? 'VNPAY đã báo thanh toán thành công nhưng hợp đồng không còn đủ điều kiện nhận cọc. Vui lòng liên hệ quản trị viên để đối soát; không thanh toán lại.'
        : waiting
          ? 'Đang chờ VNPAY xác nhận thanh toán với hệ thống…'
          : 'Chưa nhận được xác nhận từ VNPAY. Vui lòng kiểm tra lại sau; chưa thể kết luận thanh toán thành công.';

  return <main className="container" style={{ padding: '48px 24px', maxWidth: 800 }}>
    <h1>Kết quả thanh toán VNPAY</h1>
    <p>{error || message}</p>
    {payment?.status === 'PENDING' && verificationMessage && <p>{verificationMessage}</p>}
    {payment?.status === 'PENDING' && payment.nextQueryAt && <p>Có thể đối soát lại từ {new Date(payment.nextQueryAt).toLocaleTimeString('vi-VN')}.</p>}
    {payment && <p>Tiền cọc: <strong>{payment.amount.toLocaleString('vi-VN')} đ</strong></p>}
    {reference && <p>Mã giao dịch: {reference}</p>}
    {!waiting && (!payment || payment.status === 'PENDING') &&
      <button className="btn btn-primary" onClick={() => { setWaiting(true); setRefresh(v => v + 1); }}>Kiểm tra lại</button>}
    <p><Link to="/contracts">Về danh sách hợp đồng</Link></p>
    <p>Đang sử dụng môi trường VNPAY Sandbox để thử nghiệm.</p>
  </main>;
}
