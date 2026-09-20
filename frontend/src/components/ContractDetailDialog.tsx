import { useEffect, useRef, type ReactNode } from 'react';
import { FileText, X, ShieldCheck, Clock } from 'lucide-react';
import type { Contract } from '../types/contract';
import './ContractDetailDialog.css';
import ContractClosurePanel from './ContractClosurePanel';

const date = (value?: string) => value ? new Date(value).toLocaleString('vi-VN', value.length === 10 ? { dateStyle: 'medium' } : { dateStyle: 'medium', timeStyle: 'short' }) : 'Chưa có';
const money = (value: number) => `${value.toLocaleString('vi-VN')} đ`;

export default function ContractDetailDialog({ contract: c, userId, busy, accepted, onAccept, onClose, onSign, statusBadge, onBusy, onUpdated }: {
  contract: Contract; userId?: string; busy: boolean; accepted: boolean;
  onBusy: (busy: boolean) => void; onUpdated: (contract: Contract) => void;
  onAccept: (value: boolean) => void; onClose: () => void; onSign: () => void; statusBadge: ReactNode;
}) {
  const dialog = useRef<HTMLDialogElement>(null);
  useEffect(() => {
    const previous = document.activeElement as HTMLElement | null;
    const element = dialog.current;
    element?.showModal();
    return () => { element?.close(); previous?.focus(); };
  }, []);
  const canSign = Boolean(c.documentContent && c.status === 'AWAITING_SIGNATURES' && !c.closureRequests?.some(r => r.status === 'PENDING') && (
    (userId === c.tenantId && !c.tenantSignedAt) || (userId === c.landlordId && !c.landlordSignedAt)
  ));
  const milestones = [
    ['Gửi yêu cầu thuê', c.createdAt], ['Thanh toán cọc giả lập', c.depositPaidAt],
    ['Tạo văn bản hợp đồng', c.formalizedAt], ['Người thuê ký giả lập', c.tenantSignedAt],
    ['Chủ nhà ký giả lập', c.landlordSignedAt], ['Hợp đồng có hiệu lực', c.activatedAt],
  ];
  return <dialog ref={dialog} className="contract-detail" aria-labelledby="contract-detail-title" onCancel={event => { event.preventDefault(); if (!busy) onClose(); }}>
    <header className="contract-detail-header">
      <div><span className="contract-detail-eyebrow"><FileText size={16} /> HỒ SƠ THUÊ PHÒNG</span>
        <h2 id="contract-detail-title">{c.contractCode || c.contractNumber || c.requestCode || c.id.slice(0, 8)}</h2>
        {statusBadge}
      </div>
      <button autoFocus className="btn btn-outline" aria-label="Đóng chi tiết hợp đồng" disabled={busy} onClick={onClose}><X size={18} /></button>
    </header>
    <div className="contract-detail-body">
      <div className="contract-detail-notice">Thanh toán và chữ ký trên hệ thống là giả lập. Dữ liệu được tự động cập nhật khi hồ sơ thay đổi.</div>
      <section><h3>Thông tin phòng & thời hạn</h3><h4>{c.roomTitle || c.room?.title || 'Phòng trọ'}</h4>
        <p>{[c.roomAddress || c.room?.address, c.roomDistrict || c.room?.district].filter(Boolean).join(', ') || 'Chưa có địa chỉ'}</p>
        <dl className="contract-detail-grid"><div><dt>Ngày bắt đầu</dt><dd>{date(c.startDate)}</dd></div><div><dt>Ngày kết thúc</dt><dd>{date(c.endDate)}</dd></div><div><dt>Mã yêu cầu</dt><dd>{c.requestCode || 'Chưa có'}</dd></div><div><dt>Cập nhật gần nhất</dt><dd>{date(c.updatedAt)}</dd></div></dl>
      </section>
      <ContractClosurePanel contract={c} userId={userId} busy={busy} onBusy={onBusy} onUpdated={onUpdated} />
      <section><h3>Các bên tham gia</h3><div className="contract-detail-grid">
        {[{ role: 'Bên cho thuê', name: c.landlordName || c.landlord?.fullName, phone: c.landlordPhone || c.landlord?.phoneNumber, email: c.landlordEmail || c.landlord?.email },
          { role: 'Bên thuê', name: c.tenantName || c.tenant?.fullName, phone: c.tenantPhone || c.tenant?.phoneNumber, email: c.tenantEmail || c.tenant?.email }].map(p => <div className="contract-detail-party" key={p.role}><span>{p.role}</span><h4>{p.name || 'Chưa có thông tin'}</h4><p>{p.phone || 'Chưa có số điện thoại'}</p><p>{p.email || 'Chưa có email'}</p></div>)}
      </div></section>
      <section><h3>Chi phí & tiền cọc</h3><dl className="contract-detail-grid">
        <div><dt>Tiền thuê / tháng</dt><dd>{money(c.monthlyRent)}</dd></div><div><dt>Tiền cọc thỏa thuận</dt><dd>{money(c.depositAmount)}</dd></div>
        <div><dt>Tiền thuê tháng đầu + tiền cọc</dt><dd>{money(c.monthlyRent + c.depositAmount)}</dd></div><div><dt>Trạng thái cọc</dt><dd>{c.depositPaidAt ? 'Đã thanh toán giả lập' : 'Chưa thanh toán'}</dd></div>
        {c.depositDeadline && <div><dt>Hạn thanh toán cọc</dt><dd>{date(c.depositDeadline)}</dd></div>}
        {c.paymentReference && <div><dt>Mã giao dịch giả lập</dt><dd>{c.paymentReference}</dd></div>}
      </dl></section>
      <section><h3>Tiến trình hồ sơ</h3><ol className="contract-detail-timeline">{milestones.map(([label, at]) => <li key={label} className={at ? 'completed' : ''}>{at ? <ShieldCheck size={19} /> : <Clock size={19} />}<div><strong>{label}</strong><span>{at ? date(at) : 'Chưa ghi nhận'}</span></div></li>)}</ol>
        {c.cancellationReason && <div className="contract-detail-reason"><strong>Lý do từ chối / hủy / chấm dứt</strong><p>{c.cancellationReason}</p></div>}
      </section>
      {c.terms && <section><h3>Ghi chú & thỏa thuận</h3><p className="contract-detail-text">{c.terms}</p></section>}
      <section><h3>Nội dung hợp đồng</h3>{c.documentContent ? <div className="contract-detail-document">{c.documentContent}</div> : <p>Chưa có văn bản hợp đồng. Hệ thống sẽ tạo nội dung sau khi thanh toán cọc giả lập thành công.</p>}</section>
      <section><h3>Xác nhận của hai bên</h3><div className="contract-detail-grid">{[['Người thuê', c.tenantSignedAt], ['Chủ nhà', c.landlordSignedAt]].map(([label, at]) => <div className="contract-detail-party" key={label}><strong>{label}</strong><p>{at ? 'Đã ký giả lập' : 'Chưa ký'}</p>{at && <small>{date(at)}</small>}</div>)}</div>
        {canSign && <label className="contract-detail-consent"><input type="checkbox" checked={accepted} disabled={busy} onChange={e => onAccept(e.target.checked)} /> Tôi đã đọc nội dung và đồng ý ký giả lập bằng tài khoản của mình.</label>}
      </section>
    </div>
    <footer className="contract-detail-footer"><span>{busy ? 'Đang xử lý, vui lòng đợi…' : 'Hồ sơ được lưu trên hệ thống'}</span><button className="btn btn-outline" disabled={busy} onClick={onClose}>Đóng</button>{canSign && <button className="btn btn-primary" disabled={busy || !accepted} onClick={onSign}>Xác nhận ký (giả lập)</button>}</footer>
  </dialog>;
}
