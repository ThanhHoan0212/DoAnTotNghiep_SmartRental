import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Badge, Empty, LoadState, Pager } from '../components/AdminShared';
import { useRemote } from '../hooks/useRemote';
import type { Complaint, ComplaintStatus } from '../services/adminService';
import { date, labels } from '../services/adminFormat';
import type { PageResponse } from '../services/roomService';
import './AdminPage.css';

const statuses: ComplaintStatus[] = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'REJECTED'];

export default function SupportResultsPage() {
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(0);
  const [revision, setRevision] = useState(0);
  const query = new URLSearchParams({ page: String(page), size: '10' });
  if (status) query.set('status', status);
  const { data, loading, error } = useRemote<PageResponse<Complaint>>(`/complaints/mine?${query}`, revision);
  useEffect(() => {
    const refresh = () => { if (document.visibilityState === 'visible') setRevision(value => value + 1); };
    window.addEventListener('focus', refresh);
    document.addEventListener('visibilitychange', refresh);
    const timer = window.setInterval(refresh, 30000);
    return () => {
      window.removeEventListener('focus', refresh);
      document.removeEventListener('visibilitychange', refresh);
      window.clearInterval(timer);
    };
  }, []);

  return <main className="admin-public">
    <div className="admin-page-title">
      <div><h1>Kết quả khiếu nại & hỗ trợ hợp đồng</h1><p>Theo dõi tiến độ và xem phản hồi của quản trị viên cho những yêu cầu bạn đã gửi.</p></div>
      <Link to="/contracts" className="btn btn-outline">Hỗ trợ hợp đồng</Link>
    </div>
    <p className="admin-note">Để gửi yêu cầu mới, vào Hợp đồng và chọn “Hỗ trợ hợp đồng” tại hợp đồng cần hỗ trợ.</p>
    <p className="admin-note">Hiển thị tất cả yêu cầu do bạn gửi ở mọi hợp đồng, bao gồm yêu cầu cũ chưa gắn hợp đồng. Kết quả tự cập nhật mỗi 30 giây và khi bạn quay lại trang.</p>
    <div className="admin-filters">
      <label>Trạng thái xử lý<select value={status} onChange={e => { setStatus(e.target.value); setPage(0); }}>
        <option value="">Tất cả trạng thái</option>
        {statuses.map(value => <option key={value} value={value}>{labels[value]}</option>)}
      </select></label>
      <button type="button" disabled={loading} onClick={() => setRevision(value => value + 1)}>Làm mới kết quả</button>
    </div>
    <LoadState loading={loading} error={error} retry={() => setRevision(value => value + 1)} />
    {data && <>
      {!data.content.length && <Empty>{status ? 'Chưa có yêu cầu phù hợp với trạng thái đã chọn.' : 'Bạn chưa gửi yêu cầu khiếu nại hoặc hỗ trợ hợp đồng.'}</Empty>}
      {data.content.map(item => <details key={item.id} className="support-result">
        <summary>{item.subject} <Badge value={item.status} /><span className="admin-note"> · Xem chi tiết kết quả</span></summary>
        <p className="admin-note">Hợp đồng: {item.contractCode || item.contractId || 'Yêu cầu cũ chưa gắn hợp đồng'} · Phòng: {item.roomTitle || '—'}</p>
        <p className="admin-note">{labels[item.category]} · Gửi lúc: {date(item.createdAt)} · Cập nhật: {date(item.updatedAt)}</p>
        <h3>Nội dung yêu cầu</h3>
        <p className="admin-prose">{item.description}</p>
        <div className="admin-response">
          <h3>Kết quả xử lý: {labels[item.status]}</h3>
          {item.response ? <><p className="admin-prose">{item.response}</p><small>Phản hồi từ: {item.handledBy || 'Quản trị viên'} · {date(item.updatedAt)}</small></>
            : <p>{item.status === 'OPEN' ? 'Yêu cầu đã được gửi, đang chờ quản trị viên tiếp nhận.' : item.status === 'IN_PROGRESS' ? 'Quản trị viên đang xử lý yêu cầu. Vui lòng theo dõi kết quả tại đây.' : 'Chưa có nội dung phản hồi từ quản trị viên.'}</p>}
        </div>
      </details>)}
      <Pager data={data} change={setPage} />
    </>}
  </main>;
}
