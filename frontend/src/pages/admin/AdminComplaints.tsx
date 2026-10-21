import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Badge, Empty, LoadState, Modal, Pager } from '../../components/AdminShared';
import { useRemote } from '../../hooks/useRemote';
import { date, labels, message } from '../../services/adminFormat';
import { adminService } from '../../services/adminService';
import type { Complaint, ComplaintStatus } from '../../services/adminService';
import type { PageResponse } from '../../services/roomService';

export default function AdminComplaints() {
  const [page, setPage] = useState(0), [filter, setFilter] = useState(''), [revision, setRevision] = useState(0);
  const [selected, setSelected] = useState<Complaint | null>(null), [response, setResponse] = useState('');
  const [status, setStatus] = useState<ComplaintStatus>('IN_PROGRESS');
  const [busy, setBusy] = useState(false), [error, setError] = useState(''), [success, setSuccess] = useState('');
  const { data, loading, error: loadError } = useRemote<PageResponse<Complaint>>(`/admin/complaints?${new URLSearchParams({ page: String(page), size: '10', ...(filter ? { status: filter } : {}) })}`, revision);
  return <><div className="admin-page-title"><div><h1>Xử lý khiếu nại</h1><p>Tiếp nhận phản ánh và gửi kết quả xử lý cho người dùng.</p></div></div>
    <div className="admin-filters"><label>Trạng thái<select value={filter} onChange={e => { setFilter(e.target.value); setPage(0); }}><option value="">Tất cả khiếu nại</option>{['OPEN','IN_PROGRESS','RESOLVED','REJECTED'].map(s => <option key={s} value={s}>{labels[s]}</option>)}</select></label></div>
    {success && <p className="admin-success" role="status">{success}</p>}
    <LoadState loading={loading} error={loadError} retry={() => setRevision(x => x + 1)} />
    {data && <div className="admin-panel"><div className="admin-table-wrap"><table><thead><tr><th>Khiếu nại</th><th>Người gửi</th><th>Trạng thái</th><th>Tiếp nhận</th><th>Thao tác</th></tr></thead><tbody>
      {data.content.map(item => <tr key={item.id}><td><strong>{item.subject}</strong><small>{labels[item.category]}</small><small>{item.roomTitle}</small>{item.contractCode && <small>Hợp đồng: {item.contractCode}</small>}</td><td>{item.reporterName}<small>{item.reporterEmail}</small></td><td><Badge value={item.status} /></td><td>{date(item.createdAt)}</td><td><button onClick={() => { setSelected(item); setResponse(item.response || ''); setStatus(item.status === 'OPEN' ? 'IN_PROGRESS' : item.status); setError(''); }}>Xem và phản hồi</button></td></tr>)}
    </tbody></table></div>{!data.content.length && <Empty>Chưa có khiếu nại trong trạng thái này.</Empty>}<Pager data={data} change={setPage} /></div>}
    {selected && <Modal title="Chi tiết khiếu nại" close={() => { if (!busy) setSelected(null); }}>
      <h3>{selected.subject}</h3><p>{selected.reporterName} · {selected.reporterEmail}</p><Badge value={selected.category} />
      {selected.contractId && <p><Link className="admin-title-link" to={`/contracts?contractId=${selected.contractId}`}>Hợp đồng: {selected.contractCode || selected.contractId.slice(0, 8)}</Link></p>}
      {selected.roomId && <p><Link className="admin-title-link" to={`/rooms/${selected.roomId}`}>Xem phòng: {selected.roomTitle}</Link></p>}
      <p className="admin-prose">{selected.description}</p><small>Gửi lúc {date(selected.createdAt)} · Cập nhật {date(selected.updatedAt)}</small>
      {selected.handledBy && <p>Người xử lý gần nhất: {selected.handledBy}</p>}
      <form className="admin-form" onSubmit={async e => {
        e.preventDefault(); setBusy(true); setError('');
        try { await adminService.handleComplaint(selected, status, response); setSelected(null); setRevision(x => x + 1); setSuccess('Đã gửi phản hồi. Người dùng xem kết quả trong phần hỗ trợ của hợp đồng tương ứng.'); }
        catch (err) { setError(message(err)); } finally { setBusy(false); }
      }}>
        <label>Kết quả xử lý<select value={status} onChange={e => setStatus(e.target.value as ComplaintStatus)}>{['IN_PROGRESS','RESOLVED','REJECTED'].map(s => <option value={s} key={s}>{labels[s]}</option>)}</select></label>
        <label>Phản hồi cho người gửi<textarea required maxLength={5000} rows={5} value={response} onChange={e => setResponse(e.target.value)} placeholder="Nêu rõ kết quả kiểm tra và hướng xử lý…" /></label>
        {error && <div className="admin-alert" role="alert">{error}</div>}
        <button disabled={busy} className="admin-primary">{busy ? 'Đang gửi…' : 'Lưu phản hồi'}</button>
      </form>
    </Modal>}
  </>;
}
