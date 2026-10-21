import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Empty, LoadState } from '../../components/AdminShared';
import { useRemote } from '../../hooks/useRemote';
import { labels, money } from '../../services/adminFormat';
import type { Analytics, Distribution } from '../../services/adminService';

function DistributionChart({ title, items, prices = false }: { title: string; items: Distribution[]; prices?: boolean }) {
  const max = Math.max(1, ...items.map(x => x.count));
  return <section className="admin-panel admin-chart"><h2>{title}</h2>{!items.length && <Empty />}
    {items.map(item => <div className="admin-bar-row" key={item.label}><div><span>{labels[item.label] || item.label}</span><strong>{item.count.toLocaleString('vi-VN')}</strong></div><div className="admin-bar-track"><span style={{ width: `${item.count / max * 100}%` }} /></div>{prices && <small>Giá thuê trung bình: {money(item.averageRent || 0)}</small>}</div>)}
  </section>;
}
function exportCsv(report: Analytics) {
  const rows = [['Từ ngày', report.from, 'Đến ngày', report.to],
    ['Tháng','Tài khoản mới','Tin đăng mới','Hợp đồng tạo mới','Tiền cọc VNPay thành công (VNĐ)'],
    ...report.monthly.map(m => [m.month, m.users, m.rooms, m.contracts, m.depositCollected])];
  const csv = rows.map(row => row.map(cell => `"${String(cell).replace(/"/g, '""')}"`).join(',')).join('\r\n');
  const url = URL.createObjectURL(new Blob(['\uFEFF', csv], { type: 'text/csv;charset=utf-8;' }));
  const link = document.createElement('a'); link.href = url; link.download = `bao-cao-${report.from}-${report.to}.csv`; link.click();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
export default function AdminAnalytics({ overview = false }: { overview?: boolean }) {
  const [from, setFrom] = useState(''), [to, setTo] = useState(''), [range, setRange] = useState('');
  const [revision, setRevision] = useState(0);
  const { data, loading, error } = useRemote<Analytics>(`/admin/analytics${range ? `?${range}` : ''}`, revision);
  const max = Math.max(1, ...(data?.monthly.flatMap(m => [m.rooms, m.users, m.contracts]) || []));
  return <>
    <div className="admin-page-title"><div><h1>{overview ? 'Tổng quan quản trị' : 'Báo cáo & phân tích'}</h1><p>{overview ? 'Theo dõi hoạt động SmartRental và những việc cần xử lý.' : 'Theo dõi tăng trưởng, nguồn cung phòng và giao dịch đặt cọc.'}</p></div><button onClick={() => setRevision(x => x + 1)}>Làm mới dữ liệu</button></div>
    <LoadState loading={loading} error={error} retry={() => setRevision(x => x + 1)} />
    {data && <><p className="admin-section-label">Hiện trạng toàn hệ thống</p><div className="admin-metrics">
      {[
        ['Tài khoản', data.snapshot.totalUsers, `${data.snapshot.activeUsers} đang hoạt động`, '/admin/users'],
        ['Phòng cho thuê', data.snapshot.totalRooms, `${data.snapshot.rentedRooms} đã cho thuê`, '/admin/rooms'],
        ['Tin chờ duyệt', data.snapshot.pendingRooms, 'Cần kiểm tra nội dung', '/admin/rooms'],
        ['Khiếu nại cần xử lý', data.snapshot.openComplaints, 'Mới tiếp nhận và đang xử lý', '/admin/complaints'],
      ].map(([label, value, note, href]) => <Link className="admin-metric" key={String(label)} to={String(href)}><span>{label}</span><strong>{Number(value).toLocaleString('vi-VN')}</strong><small>{note}</small></Link>)}
    </div><div className="admin-summary-strip"><span>Hợp đồng đang hoạt động: <strong>{data.snapshot.activeContracts}</strong></span><span>Giá thuê trung bình: <strong>{money(data.snapshot.averageRent)}</strong></span></div></>}
    <form className="admin-filters" onSubmit={e => { e.preventDefault(); setRange(new URLSearchParams({ ...(from ? { from } : {}), ...(to ? { to } : {}) }).toString()); }}>
      <label>Từ ngày<input type="date" value={from} max={to || undefined} onChange={e => setFrom(e.target.value)} /></label><label>Đến ngày<input type="date" value={to} min={from || undefined} onChange={e => setTo(e.target.value)} /></label><button type="submit">Xem báo cáo</button>{data && <button type="button" onClick={() => exportCsv(data)}>Xuất CSV</button>}
    </form>
    {data && <><p className="admin-note">Dữ liệu phát sinh từ {data.from} đến hết {data.to} (giờ Việt Nam). Mặc định 6 tháng gần nhất. Chỉ số hiện trạng phía trên và phân bố phòng, tài khoản phản ánh toàn hệ thống.</p>
      <section className="admin-panel admin-chart"><h2>Tăng trưởng theo tháng</h2><div className="admin-chart-legend"><span className="legend-users">Tài khoản mới</span><span className="legend-rooms">Tin đăng mới</span><span className="legend-contracts">Hợp đồng tạo mới</span></div>
        <div className="admin-month-chart" role="img" aria-label="Biểu đồ tăng trưởng theo tháng, số liệu chi tiết ở bảng bên dưới">{data.monthly.map(m => <div className="admin-month" key={m.month}><div className="admin-month-bars">{(['users','rooms','contracts'] as const).map(key => <span key={key} className={`month-${key}`} title={`${m.month}: ${m[key]}`} style={{ height: `${m[key] / max * 140}px`, minHeight: m[key] ? 3 : 0 }} />)}</div><small>{m.month}</small></div>)}</div>
        <div className="admin-table-wrap"><table><thead><tr><th>Tháng</th><th>Tài khoản mới</th><th>Tin đăng mới</th><th>Hợp đồng tạo mới</th><th>Tiền cọc VNPay thành công</th></tr></thead><tbody>{data.monthly.map(m => <tr key={m.month}><td>{m.month}</td><td>{m.users}</td><td>{m.rooms}</td><td>{m.contracts}</td><td>{money(m.depositCollected)}</td></tr>)}</tbody><tfoot><tr><th>Tổng</th><th>{data.monthly.reduce((s, m) => s + m.users, 0)}</th><th>{data.monthly.reduce((s, m) => s + m.rooms, 0)}</th><th>{data.monthly.reduce((s, m) => s + m.contracts, 0)}</th><th>{money(data.monthly.reduce((s, m) => s + m.depositCollected, 0))}</th></tr></tfoot></table></div>
        <p className="admin-note">Tiền cọc tính theo giao dịch VNPay thành công tại thời điểm hoàn tất; đây là tiền cọc thu hộ, không phải doanh thu của nền tảng. Giá thuê trung bình tính trên phòng đã duyệt, đang giữ chỗ và đã cho thuê.</p>
      </section>
      <div className="admin-chart-grid"><DistributionChart title="Trạng thái tin đăng · hiện tại" items={data.roomStatuses} /><DistributionChart title="Cơ cấu tài khoản · hiện tại" items={data.userRoles} /><DistributionChart title="Nguồn cung theo quận · hiện tại" items={data.districts} prices /><DistributionChart title="Trạng thái hợp đồng tạo trong kỳ" items={data.contractStatuses} /></div>
    </>}
  </>;
}
