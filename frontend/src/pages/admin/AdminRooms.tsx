import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Badge, Empty, LoadState, Modal, Pager } from '../../components/AdminShared';
import { useRemote } from '../../hooks/useRemote';
import { date, labels, message, money } from '../../services/adminFormat';
import { adminService } from '../../services/adminService';
import { roomService } from '../../services/roomService';
import { api } from '../../services/api';
import type { PageResponse } from '../../services/roomService';
import type { RoomDetail, RoomStatus, RoomSummary } from '../../types/room';

export default function AdminRooms() {
  const [page, setPage] = useState(0), [status, setStatus] = useState(''), [district, setDistrict] = useState('');
  const [districtInput, setDistrictInput] = useState(''), [revision, setRevision] = useState(0);
  const [busy, setBusy] = useState(false), [error, setError] = useState(''), [success, setSuccess] = useState('');
  const [editing, setEditing] = useState<RoomDetail | null>(null);
  const { data, loading, error: loadError } = useRemote<PageResponse<RoomSummary>>(`/rooms/admin/all?${new URLSearchParams({ page: String(page), size: '10', ...(status ? { status } : {}), ...(district ? { district } : {}) })}`, revision);
  async function act(action: () => Promise<unknown>, text: string) {
    setBusy(true); setError(''); setSuccess('');
    try { await action(); setSuccess(text); setEditing(null); setRevision(x => x + 1); }
    catch (err) { setError(message(err)); } finally { setBusy(false); }
  }
  async function edit(id: string) {
    setBusy(true); setError('');
    try { setEditing((await api.get<RoomDetail>(`/admin/rooms/${id}`)).data); } catch (err) { setError(message(err)); } finally { setBusy(false); }
  }
  function moderate(room: RoomSummary, next: RoomStatus) {
    if (window.confirm(`${labels[next]} tin “${room.title}”?`)) void act(() => adminService.roomStatus(room.id, next), 'Đã cập nhật trạng thái tin đăng.');
  }
  return <>
    <div className="admin-page-title"><div><h1>Quản lý phòng cho thuê</h1><p>Kiểm duyệt và cập nhật tin đăng trong toàn hệ thống.</p></div><Link className="admin-primary" to="/post-room">+ Đăng phòng</Link></div>
    <form className="admin-filters" onSubmit={e => { e.preventDefault(); setDistrict(districtInput.trim()); setPage(0); }}>
      <label>Trạng thái<select value={status} onChange={e => { setStatus(e.target.value); setPage(0); }}><option value="">Tất cả trạng thái</option>{['PENDING','APPROVED','REJECTED','HIDDEN','RESERVED','RENTED'].map(s => <option key={s} value={s}>{labels[s]}</option>)}</select></label>
      <label>Quận / huyện<input value={districtInput} onChange={e => setDistrictInput(e.target.value)} placeholder="Nhập quận / huyện" /></label><button type="submit">Lọc tin đăng</button>
    </form>
    {error && !editing && <div className="admin-alert" role="alert">{error}</div>}{success && <p className="admin-success" role="status">{success}</p>}
    <LoadState loading={loading} error={loadError} retry={() => setRevision(x => x + 1)} />
    {data && <div className="admin-panel"><div className="admin-table-wrap"><table><thead><tr><th>Phòng cho thuê</th><th>Giá / diện tích</th><th>Trạng thái</th><th>Ngày đăng</th><th>Thao tác</th></tr></thead><tbody>
      {data.content.map(room => <tr key={room.id}><td><Link className="admin-title-link" to={`/rooms/${room.id}`}>{room.title}</Link><small>{room.address} · {room.district}</small></td><td><strong>{money(room.price)}</strong><small>{room.area} m² · {room.viewCount} lượt xem</small></td><td><Badge value={room.status} /></td><td>{date(room.createdAt)}</td><td><div className="admin-row-actions">
        <button disabled={busy} onClick={() => void edit(room.id)}>Chi tiết / sửa</button>
        {!['RESERVED','RENTED'].includes(room.status) && <>{room.status !== 'APPROVED' && <button disabled={busy} onClick={() => moderate(room, 'APPROVED')}>Duyệt</button>}
          {room.status === 'PENDING' && <button disabled={busy} onClick={() => moderate(room, 'REJECTED')}>Từ chối</button>}
          {room.status === 'APPROVED' && <button disabled={busy} onClick={() => moderate(room, 'HIDDEN')}>Ẩn tin</button>}</>}
        {!['RESERVED','RENTED'].includes(room.status) && <button className="admin-danger" disabled={busy} onClick={() => {
          if (window.confirm(`Xóa tin “${room.title}”? Chỉ xóa được phòng chưa phát sinh hợp đồng.`)) void act(async () => {
            await roomService.deleteRoom(room.id);
            if (data.content.length === 1 && page > 0) setPage(page - 1);
          }, 'Đã xóa tin đăng.');
        }}>Xóa</button>}
      </div></td></tr>)}
    </tbody></table></div>{!data.content.length && <Empty />}<Pager data={data} change={setPage} /></div>}
    <p className="admin-note">Phòng đang giữ chỗ hoặc đã cho thuê được quản lý trạng thái thông qua hợp đồng.</p>
    {editing && <Modal title="Chi tiết và chỉnh sửa phòng" close={() => { if (!busy) setEditing(null); }}>
      <p>Chủ nhà: <strong>{editing.landlord.fullName}</strong> · {editing.landlord.email}</p>
      {editing.images[0] && <img className="admin-room-image" src={editing.images[0].imageUrl} alt={editing.title} />}
      <form className="admin-form" onSubmit={e => { e.preventDefault(); void act(() => roomService.updateRoom(editing.id, { title: editing.title, description: editing.description, price: editing.price, area: editing.area, address: editing.address, district: editing.district }), 'Đã lưu thông tin phòng.'); }}>
        <label>Tiêu đề<input required maxLength={255} value={editing.title} onChange={e => setEditing({ ...editing, title: e.target.value })} /></label>
        <div className="admin-form-grid"><label>Giá thuê (VNĐ / tháng)<input type="number" min="1" required value={editing.price} onChange={e => setEditing({ ...editing, price: Number(e.target.value) })} /></label><label>Diện tích (m²)<input type="number" min="1" step="0.1" required value={editing.area} onChange={e => setEditing({ ...editing, area: Number(e.target.value) })} /></label></div>
        <label>Địa chỉ<input required maxLength={255} value={editing.address} onChange={e => setEditing({ ...editing, address: e.target.value })} /></label>
        <label>Quận / huyện<input required maxLength={100} value={editing.district} onChange={e => setEditing({ ...editing, district: e.target.value })} /></label>
        <label>Mô tả<textarea rows={6} value={editing.description || ''} onChange={e => setEditing({ ...editing, description: e.target.value })} /></label>
        {error && <div className="admin-alert" role="alert">{error}</div>}
        <button className="admin-primary" disabled={busy}>{busy ? 'Đang lưu…' : 'Lưu thay đổi'}</button>
      </form>
    </Modal>}
  </>;
}
