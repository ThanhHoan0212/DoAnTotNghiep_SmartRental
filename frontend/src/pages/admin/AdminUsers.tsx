import { useState } from 'react';
import { Badge, Empty, LoadState, Pager } from '../../components/AdminShared';
import { useRemote } from '../../hooks/useRemote';
import { date, labels, message } from '../../services/adminFormat';
import { userService } from '../../services/userService';
import type { PageResponse } from '../../services/roomService';
import type { User, UserStatus } from '../../types/auth';

export default function AdminUsers() {
  const [page, setPage] = useState(0), [role, setRole] = useState(''), [status, setStatus] = useState('');
  const [keyword, setKeyword] = useState(''), [input, setInput] = useState(''), [revision, setRevision] = useState(0);
  const [busy, setBusy] = useState(false), [error, setError] = useState(''), [success, setSuccess] = useState('');
  const { data, loading, error: loadError } = useRemote<PageResponse<User>>(`/admin/users?${new URLSearchParams({ page: String(page), size: '10', keyword, ...(role ? { role } : {}), ...(status ? { status } : {}) })}`, revision);
  async function update(user: User, next: UserStatus) {
    if (!window.confirm(`${labels[next]} tài khoản ${user.email}?`)) return;
    setBusy(true); setError(''); setSuccess('');
    try { await userService.updateUserStatus(user.id, next); setRevision(x => x + 1); setSuccess('Đã cập nhật trạng thái tài khoản.'); }
    catch (err) { setError(message(err)); } finally { setBusy(false); }
  }
  return <><div className="admin-page-title"><div><h1>Quản lý tài khoản</h1><p>Tra cứu người dùng, kiểm tra vai trò và kiểm soát quyền sử dụng.</p></div></div>
    <form className="admin-filters" onSubmit={e => { e.preventDefault(); setKeyword(input.trim()); setPage(0); }}>
      <label>Tìm tài khoản<input value={input} onChange={e => setInput(e.target.value)} placeholder="Họ tên, email, số điện thoại" /></label>
      <label>Vai trò<select value={role} onChange={e => { setRole(e.target.value); setPage(0); }}><option value="">Tất cả vai trò</option>{['TENANT','LANDLORD','ADMIN'].map(x => <option key={x} value={x}>{labels[x]}</option>)}</select></label>
      <label>Trạng thái<select value={status} onChange={e => { setStatus(e.target.value); setPage(0); }}><option value="">Tất cả trạng thái</option>{['ACTIVE','INACTIVE','BANNED'].map(x => <option key={x} value={x}>{labels[x]}</option>)}</select></label><button>Tìm kiếm</button>
    </form>
    {error && <div className="admin-alert" role="alert">{error}</div>}{success && <p className="admin-success" role="status">{success}</p>}
    <LoadState loading={loading} error={loadError} retry={() => setRevision(x => x + 1)} />
    {data && <div className="admin-panel"><div className="admin-table-wrap"><table><thead><tr><th>Tài khoản</th><th>Vai trò</th><th>Trạng thái</th><th>Xác minh</th><th>Ngày tạo</th><th>Thao tác</th></tr></thead><tbody>
      {data.content.map(user => <tr key={user.id}><td><strong>{user.fullName}</strong><small>{user.email}</small><small>{user.phone || 'Chưa có số điện thoại'}</small></td><td><Badge value={user.role} /></td><td><Badge value={user.status} /></td><td>{user.isIdentityVerified ? 'Đã xác minh eKYC' : 'Chưa xác minh'}</td><td>{date(user.createdAt)}</td><td>{user.role === 'ADMIN' ? <small>Tài khoản quản trị được bảo vệ</small> : <div className="admin-row-actions">
        {user.status !== 'ACTIVE' && <button disabled={busy} onClick={() => void update(user, 'ACTIVE')}>Kích hoạt</button>}
        {user.status !== 'BANNED' && <button className="admin-danger" disabled={busy} onClick={() => void update(user, 'BANNED')}>Khóa tài khoản</button>}
      </div>}</td></tr>)}
    </tbody></table></div>{!data.content.length && <Empty />}<Pager data={data} change={setPage} /></div>}
    <p className="admin-note">Tài khoản bị khóa mất quyền gọi API ngay cả khi phiên đăng nhập cũ vẫn còn thời hạn.</p>
  </>;
}
