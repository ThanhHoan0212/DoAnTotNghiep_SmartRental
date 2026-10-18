import { useState } from 'react';
import { api } from '../services/api';
import type { Complaint, ComplaintCategory } from '../services/adminService';
import type { PageResponse } from '../services/roomService';
import type { Contract } from '../types/contract';
import { Badge, Empty, LoadState, Pager } from './AdminShared';
import { useRemote } from '../hooks/useRemote';
import { date, labels, message } from '../services/adminFormat';
import '../pages/AdminPage.css';

export default function ContractSupportPanel({ contract, onBusy }: { contract: Contract; onBusy?: (busy: boolean) => void }) {
  const [subject, setSubject] = useState(''), [category, setCategory] = useState<ComplaintCategory>('OTHER');
  const [description, setDescription] = useState(''), [busy, setBusy] = useState(false), [error, setError] = useState(''), [success, setSuccess] = useState('');
  const [page, setPage] = useState(0), [revision, setRevision] = useState(0);
  const { data, loading, error: loadError } = useRemote<PageResponse<Complaint>>(`/complaints/mine?contractId=${contract.id}&page=${page}&size=5`, revision);
  return <div className="contract-support">
    <p>Gửi yêu cầu đến quản trị viên và theo dõi phản hồi cho hợp đồng <strong>{contract.contractCode || contract.contractNumber || contract.id.slice(0, 8)}</strong>.</p>
    <p className="admin-note">Phòng: {contract.roomTitle || contract.room?.title} · Chỉ hiển thị những yêu cầu do bạn gửi.</p>
    <form className="admin-form" onSubmit={async e => {
      e.preventDefault(); setBusy(true); onBusy?.(true); setError(''); setSuccess('');
      try {
        await api.post('/complaints', { subject, category, description, contractId: contract.id });
        setSubject(''); setDescription(''); setPage(0); setRevision(x => x + 1);
        setSuccess('Đã gửi yêu cầu. Phản hồi của quản trị viên sẽ hiển thị bên dưới.');
      } catch (err) { setError(message(err)); } finally { setBusy(false); onBusy?.(false); }
    }}>
      <label>Tiêu đề<input required maxLength={200} value={subject} disabled={busy} onChange={e => setSubject(e.target.value)} placeholder="Vấn đề cần hỗ trợ về hợp đồng" /></label>
      <label>Loại yêu cầu<select value={category} disabled={busy} onChange={e => setCategory(e.target.value as ComplaintCategory)}><option value="OTHER">Điều khoản / vấn đề khác</option><option value="PAYMENT">Thanh toán / tiền cọc</option><option value="LISTING">Phòng không đúng thỏa thuận</option><option value="FRAUD">Nghi ngờ lừa đảo</option></select></label>
      <label>Nội dung<textarea required minLength={10} maxLength={5000} rows={4} disabled={busy} value={description} onChange={e => setDescription(e.target.value)} placeholder="Mô tả vấn đề và hướng xử lý mong muốn (ít nhất 10 ký tự)…" /></label>
      {error && <div className="admin-alert" role="alert">{error}</div>}{success && <p className="admin-success" role="status">{success}</p>}
      <button className="admin-primary" disabled={busy}>{busy ? 'Đang gửi…' : 'Gửi yêu cầu hỗ trợ'}</button>
    </form>
    <div className="contract-support-history"><h3>Yêu cầu hỗ trợ của tôi</h3><button type="button" disabled={loading} onClick={() => setRevision(x => x + 1)}>Làm mới phản hồi</button></div>
    <LoadState loading={loading} error={loadError} retry={() => setRevision(x => x + 1)} />
    {data && <>{!data.content.length && <Empty>Chưa có yêu cầu hỗ trợ cho hợp đồng này.</Empty>}{data.content.map(item => <details className="contract-support-ticket" key={item.id}><summary>{item.subject} <Badge value={item.status} /></summary><p><small>{labels[item.category]} · {date(item.createdAt)}</small></p><p className="admin-prose">{item.description}</p>{item.response ? <div className="contract-support-response"><strong>Phản hồi từ {item.handledBy}</strong><p className="admin-prose">{item.response}</p><small>Cập nhật: {date(item.updatedAt)}</small></div> : <p>Đang chờ quản trị viên tiếp nhận.</p>}</details>)}<Pager data={data} change={setPage} /></>}
  </div>;
}
