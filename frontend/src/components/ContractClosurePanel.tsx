import { useState, type FormEvent } from 'react';
import type { Contract, ClosureRefund } from '../types/contract';
import { contractService } from '../services/contractService';

const refundLabels = { FULL: 'Hoàn toàn bộ cọc', PARTIAL: 'Hoàn một phần cọc', NONE: 'Không hoàn cọc' };
const statusLabels = { PENDING: 'Chờ bên còn lại xác nhận', ACCEPTED: 'Đã thống nhất · chờ ngày chấm dứt', REJECTED: 'Đã từ chối', COMPLETED: 'Đã hoàn tất hủy / chấm dứt', LAPSED: 'Không còn hiệu lực do hợp đồng đã hết hạn' };
const money = (n: number) => `${n.toLocaleString('vi-VN')} đ`;
const date = (s: string) => new Date(s).toLocaleDateString('vi-VN');

export default function ContractClosurePanel({ contract: c, userId, busy, onBusy, onUpdated }: {
  contract: Contract; userId?: string; busy: boolean;
  onBusy: (busy: boolean) => void; onUpdated: (contract: Contract) => void;
}) {
  const [open, setOpen] = useState(false);
  const [reason, setReason] = useState('');
  const [endDate, setEndDate] = useState('');
  const [refund, setRefund] = useState<ClosureRefund>('FULL');
  const [partial, setPartial] = useState('');
  const [note, setNote] = useState('');
  const [responseReason, setResponseReason] = useState('');
  const [consent, setConsent] = useState<string | null>(null);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const requests = c.closureRequests || [];
  const pending = requests.find(r => r.status === 'PENDING');
  const hasOpen = requests.some(r => r.status === 'PENDING' || r.status === 'ACCEPTED');
  const isParty = Boolean(userId && (userId === c.tenantId || userId === c.landlordId));
  const active = c.status === 'ACTIVE';
  const eligible = active || (c.status === 'AWAITING_SIGNATURES' && Boolean(c.depositPaidAt));
  const amount = refund === 'FULL' ? c.depositAmount : refund === 'NONE' ? 0 : Number(partial);
  const validAmount = Number.isFinite(amount) && (refund !== 'PARTIAL' || (amount > 0 && amount < c.depositAmount));
  const execute = async (operation: () => Promise<Contract>, success: string) => {
    onBusy(true); setError(''); setMessage('');
    try { onUpdated(await operation()); setOpen(false); setConsent(null); setResponseReason(''); setMessage(success); }
    catch (err) { setError(err instanceof Error ? err.message : 'Không thể xử lý yêu cầu. Vui lòng thử lại.'); }
    finally { onBusy(false); }
  };
  const submit = (e: FormEvent) => {
    e.preventDefault();
    if (busy || !validAmount || !reason.trim() || !note.trim()) return;
    void execute(() => contractService.requestClosure(c.id, {
      reason: reason.trim(), requestedEndDate: active ? endDate : undefined,
      refundType: refund, refundAmount: amount, settlementNote: note.trim(),
    }), 'Đã gửi yêu cầu. Hợp đồng và phòng chưa thay đổi trạng thái; chờ bên còn lại xác nhận.');
  };
  if (!eligible && requests.length === 0) return null;
  return <section className="contract-closure-panel"><h3>Yêu cầu hủy & chấm dứt trước hạn</h3>
    {error && <p role="alert" className="contract-detail-reason">{error}</p>}
    {message && <p role="status">{message}</p>}
    {c.agreedEndDate && <div className="contract-detail-notice">Ngày chấm dứt đã thống nhất: <strong>{date(c.agreedEndDate)}</strong>. {active ? 'Hợp đồng vẫn có hiệu lực và phòng vẫn đang được thuê cho tới ngày này.' : 'Hợp đồng đã chấm dứt theo thỏa thuận.'}</div>}
    {pending && <div className="contract-detail-notice">{pending.requestedBy === userId ? 'Bạn đã gửi yêu cầu, đang chờ bên còn lại phản hồi.' : 'Có yêu cầu mới cần bên còn lại xem và xác nhận.'} {active ? 'Hợp đồng vẫn có hiệu lực.' : 'Phòng tiếp tục được giữ chỗ. Tạm dừng ký trong khi chờ xác nhận hủy.'}</div>}
    {[...requests].reverse().map(r => <article key={r.requestId} className="contract-closure-item">
      <h4>{r.kind === 'CANCELLATION' ? 'Yêu cầu hủy trước hiệu lực' : 'Yêu cầu chấm dứt trước hạn'}</h4>
      <p><strong>{statusLabels[r.status]}</strong></p>
      <p>Người gửi: {r.requestedBy === c.tenantId ? c.tenantName || 'Người thuê' : c.landlordName || 'Chủ nhà'} · {date(r.requestedAt)}</p>
      <p className="contract-detail-text"><strong>Lý do:</strong> {r.reason}</p>
      {r.requestedEndDate && <p>Ngày muốn kết thúc: <strong>{date(r.requestedEndDate)}</strong></p>}
      <p><strong>{refundLabels[r.refundType]}</strong> · Hoàn: {money(r.refundAmount)} · Giữ lại: {money(c.depositAmount - r.refundAmount)}</p>
      <p className="contract-detail-text"><strong>Căn cứ xử lý cọc:</strong> {r.settlementNote}</p>
      <p>Đây là {r.status === 'PENDING' ? 'phương án đề xuất' : r.status === 'REJECTED' || r.status === 'LAPSED' ? 'phương án chưa được thống nhất' : 'thỏa thuận đã được hai bên xác nhận'}, không phải xác nhận đã hoàn tiền.</p>
      {r.respondedAt && <p>Phản hồi bởi {r.respondedBy === c.tenantId ? 'người thuê' : 'chủ nhà'} ngày {date(r.respondedAt)}{r.responseReason ? ` · ${r.responseReason}` : ''}</p>}
      {r.completedAt && <p>Hoàn tất hủy / chấm dứt: {date(r.completedAt)}</p>}
      {r.status === 'PENDING' && isParty && userId !== r.requestedBy && <div className="contract-closure-form">
        <label>Lý do phản hồi (bắt buộc khi từ chối)<textarea value={responseReason} maxLength={2000} disabled={busy} onChange={e => setResponseReason(e.target.value)} rows={2} /></label>
        <label className="contract-detail-consent"><input type="checkbox" checked={consent === r.requestId} disabled={busy} onChange={e => setConsent(e.target.checked ? r.requestId : null)} /> Tôi đồng ý nội dung yêu cầu, ngày kết thúc (nếu có) và phương án tiền cọc ở trên.</label>
        <div className="contract-closure-actions"><button className="btn btn-outline" disabled={busy || !responseReason.trim()} onClick={() => void execute(() => contractService.respondClosure(c.id, r.requestId, false, responseReason.trim()), 'Đã từ chối yêu cầu. Hợp đồng tiếp tục theo trạng thái hiện tại.')}>Từ chối yêu cầu</button>
          <button className="btn btn-primary" disabled={busy || consent !== r.requestId} onClick={() => void execute(() => contractService.respondClosure(c.id, r.requestId, true, responseReason.trim() || undefined), 'Đã ghi nhận thỏa thuận của hai bên. Tiền cọc chưa được tự động hoàn.')}>Đồng ý yêu cầu</button></div>
      </div>}
    </article>)}
    {eligible && isParty && !hasOpen && !open && <button className="btn btn-outline" disabled={busy} onClick={() => { setOpen(true); setError(''); setMessage(''); }}>{active ? 'Yêu cầu chấm dứt hợp đồng trước hạn' : 'Tạo yêu cầu hủy và thỏa thuận tiền cọc'}</button>}
    {eligible && isParty && !hasOpen && open && <form className="contract-closure-form" onSubmit={submit}>
      <p>{active ? 'Chỉ khi bên còn lại đồng ý và đến ngày đã thống nhất, hợp đồng mới chấm dứt và phòng mới được mở lại.' : 'Đã thanh toán cọc: cần bên còn lại xác nhận trước khi hủy yêu cầu và mở lại phòng.'}</p>
      <label>Lý do yêu cầu<textarea required maxLength={2000} disabled={busy} value={reason} onChange={e => setReason(e.target.value)} rows={3} /></label>
      {active && <label>Ngày muốn chấm dứt<input type="date" required disabled={busy} value={endDate} onChange={e => setEndDate(e.target.value)} /><small>Không trước hôm nay/ngày bắt đầu, và phải trước ngày hết hạn {date(c.endDate)}. Hợp đồng chấm dứt từ ngày được thống nhất (giờ Việt Nam).</small></label>}
      <label>Đề xuất xử lý tiền cọc<select value={refund} disabled={busy} onChange={e => setRefund(e.target.value as ClosureRefund)}>{Object.entries(refundLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
      {refund === 'PARTIAL' && <label>Số tiền đề xuất hoàn (đ)<input type="number" required min="1" max={Math.max(0, c.depositAmount - 1)} step="1" disabled={busy} value={partial} onChange={e => setPartial(e.target.value)} /></label>}
      <p>Tiền cọc: {money(c.depositAmount)} · Đề xuất hoàn: {money(amount)}. Chưa thực hiện hoàn tiền.</p>
      <label>Điều khoản / căn cứ xử lý tiền cọc<textarea required maxLength={2000} rows={3} disabled={busy} value={note} onChange={e => setNote(e.target.value)} placeholder="Ghi rõ thỏa thuận hoàn cọc hoặc lý do giữ lại một phần/toàn bộ cọc" /></label>
      <div className="contract-closure-actions"><button type="button" className="btn btn-outline" disabled={busy} onClick={() => setOpen(false)}>Đóng biểu mẫu</button><button className="btn btn-primary" disabled={busy || !validAmount}>{busy ? 'Đang gửi…' : 'Gửi yêu cầu cho bên còn lại'}</button></div>
    </form>}
  </section>;
}
