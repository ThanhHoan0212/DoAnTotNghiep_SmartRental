import React, { useState, useEffect } from 'react';
import { useAuth } from '../hooks/useAuth';
import { userService } from '../services/userService';
import { ApiError } from '../services/api';

export default function ProfilePage() {
  const { user, updateUser } = useAuth();

  // Tab state: 'info' | 'password'
  const [activeTab, setActiveTab] = useState<'info' | 'password'>('info');

  // Profile Form state
  const [fullName, setFullName] = useState(user?.fullName || '');
  const [phone, setPhone] = useState(user?.phone || '');
  const [avatarUrl, setAvatarUrl] = useState(user?.avatarUrl || '');
  const [profileMsg, setProfileMsg] = useState<{ type: 'success' | 'error'; text: string } | null>(null);
  const [isSavingProfile, setIsSavingProfile] = useState(false);

  // Password Form state
  const [oldPassword, setOldPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [passwordMsg, setPasswordMsg] = useState<{ type: 'success' | 'error'; text: string } | null>(null);
  const [isSavingPassword, setIsSavingPassword] = useState(false);

  useEffect(() => {
    if (user) {
      setFullName(user.fullName || '');
      setPhone(user.phone || '');
      setAvatarUrl(user.avatarUrl || '');
    }
  }, [user]);

  const handleUpdateProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    setProfileMsg(null);

    if (!fullName.trim()) {
      setProfileMsg({ type: 'error', text: 'Họ và tên không được để trống' });
      return;
    }

    setIsSavingProfile(true);
    try {
      await updateUser({
        fullName: fullName.trim(),
        phone: phone.trim() || undefined,
        avatarUrl: avatarUrl.trim() || undefined,
      });
      setProfileMsg({ type: 'success', text: 'Cập nhật thông tin cá nhân thành công!' });
    } catch (err: any) {
      const text = err instanceof ApiError ? err.message : err?.message || 'Không thể cập nhật hồ sơ';
      setProfileMsg({ type: 'error', text });
    } finally {
      setIsSavingProfile(false);
    }
  };

  const handleChangePassword = async (e: React.FormEvent) => {
    e.preventDefault();
    setPasswordMsg(null);

    if (!oldPassword || !newPassword) {
      setPasswordMsg({ type: 'error', text: 'Vui lòng nhập đầy đủ mật khẩu cũ và mới' });
      return;
    }

    if (newPassword.length < 6) {
      setPasswordMsg({ type: 'error', text: 'Mật khẩu mới phải có tối thiểu 6 ký tự' });
      return;
    }

    if (newPassword !== confirmPassword) {
      setPasswordMsg({ type: 'error', text: 'Mật khẩu xác nhận không khớp' });
      return;
    }

    setIsSavingPassword(true);
    try {
      await userService.changePassword({ oldPassword, newPassword });
      setPasswordMsg({ type: 'success', text: 'Đổi mật khẩu thành công! Vui lòng ghi nhớ mật khẩu mới.' });
      setOldPassword('');
      setNewPassword('');
      setConfirmPassword('');
    } catch (err: any) {
      const text = err instanceof ApiError ? err.message : err?.message || 'Không thể đổi mật khẩu';
      setPasswordMsg({ type: 'error', text });
    } finally {
      setIsSavingPassword(false);
    }
  };

  const getRoleBadge = (role?: string) => {
    switch (role) {
      case 'ADMIN':
        return { label: 'Quản trị viên', bg: '#fee2e2', color: '#991b1b' };
      case 'LANDLORD':
        return { label: 'Chủ nhà', bg: '#e0f2fe', color: '#075985' };
      case 'TENANT':
      default:
        return { label: 'Người thuê', bg: '#dcfce7', color: '#166534' };
    }
  };

  const roleInfo = getRoleBadge(user?.role);

  return (
    <main className="container" style={{ padding: '40px 0', minHeight: '80vh' }}>
      <div style={{ maxWidth: '800px', margin: '0 auto' }}>
        {/* Header Hồ sơ */}
        <div style={{
          display: 'flex',
          alignItems: 'center',
          gap: '24px',
          padding: '28px',
          background: '#f8fafc',
          borderRadius: '16px',
          border: '1px solid #e2e8f0',
          marginBottom: '32px'
        }}>
          <div style={{
            width: '80px',
            height: '80px',
            borderRadius: '50%',
            background: user?.avatarUrl ? `url(${user.avatarUrl}) center/cover` : '#167c5a',
            color: '#ffffff',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontSize: '32px',
            fontWeight: 700,
            flexShrink: 0
          }}>
            {!user?.avatarUrl && (user?.fullName ? user.fullName[0].toUpperCase() : 'U')}
          </div>

          <div style={{ flex: 1 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px', flexWrap: 'wrap' }}>
              <h1 style={{ fontSize: '24px', margin: 0, fontWeight: 700 }}>{user?.fullName}</h1>
              <span style={{
                fontSize: '12px',
                fontWeight: 600,
                padding: '3px 10px',
                borderRadius: '12px',
                background: roleInfo.bg,
                color: roleInfo.color
              }}>
                {roleInfo.label}
              </span>
            </div>
            <p style={{ margin: '6px 0 0 0', color: '#64748b', fontSize: '14px' }}>
              {user?.email} • Tham gia từ {user?.createdAt ? new Date(user.createdAt).toLocaleDateString('vi-VN') : 'Gần đây'}
            </p>
          </div>
        </div>

        {/* Tab chuyển đổi */}
        <div style={{
          display: 'flex',
          gap: '12px',
          borderBottom: '2px solid #e2e8f0',
          marginBottom: '28px'
        }}>
          <button
            type="button"
            onClick={() => setActiveTab('info')}
            style={{
              padding: '12px 20px',
              fontWeight: 600,
              fontSize: '15px',
              border: 'none',
              background: 'none',
              borderBottom: activeTab === 'info' ? '3px solid #167c5a' : '3px solid transparent',
              color: activeTab === 'info' ? '#167c5a' : '#64748b',
              cursor: 'pointer',
              marginBottom: '-2px'
            }}
          >
            Thông tin tài khoản
          </button>

          <button
            type="button"
            onClick={() => setActiveTab('password')}
            style={{
              padding: '12px 20px',
              fontWeight: 600,
              fontSize: '15px',
              border: 'none',
              background: 'none',
              borderBottom: activeTab === 'password' ? '3px solid #167c5a' : '3px solid transparent',
              color: activeTab === 'password' ? '#167c5a' : '#64748b',
              cursor: 'pointer',
              marginBottom: '-2px'
            }}
          >
            Đổi mật khẩu
          </button>
        </div>

        {/* Nội dung Tab 1: Thông tin tài khoản */}
        {activeTab === 'info' && (
          <div style={{
            background: '#ffffff',
            padding: '28px',
            borderRadius: '12px',
            border: '1px solid #e8ecef'
          }}>
            {profileMsg && (
              <div style={{
                background: profileMsg.type === 'success' ? '#dcfce7' : '#fee2e2',
                color: profileMsg.type === 'success' ? '#166534' : '#b91c1c',
                padding: '12px 16px',
                borderRadius: '8px',
                marginBottom: '20px',
                fontSize: '14px',
                border: `1px solid ${profileMsg.type === 'success' ? '#86efac' : '#f87171'}`
              }}>
                {profileMsg.text}
              </div>
            )}

            <form onSubmit={handleUpdateProfile}>
              <div style={{ marginBottom: '18px' }}>
                <label style={{ display: 'block', fontWeight: 600, marginBottom: '6px', fontSize: '14px' }}>
                  Địa chỉ Email (Không thể thay đổi)
                </label>
                <input
                  type="email"
                  value={user?.email || ''}
                  disabled
                  style={{
                    width: '100%',
                    padding: '12px',
                    borderRadius: '8px',
                    border: '1px solid #e2e8f0',
                    background: '#f1f5f9',
                    color: '#64748b'
                  }}
                />
              </div>

              <div style={{ marginBottom: '18px' }}>
                <label style={{ display: 'block', fontWeight: 600, marginBottom: '6px', fontSize: '14px' }}>
                  Họ và tên *
                </label>
                <input
                  type="text"
                  value={fullName}
                  onChange={(e) => setFullName(e.target.value)}
                  placeholder="Nhập họ và tên"
                  required
                  style={{
                    width: '100%',
                    padding: '12px',
                    borderRadius: '8px',
                    border: '1px solid #cbd5e1'
                  }}
                />
              </div>

              <div style={{ marginBottom: '18px' }}>
                <label style={{ display: 'block', fontWeight: 600, marginBottom: '6px', fontSize: '14px' }}>
                  Số điện thoại
                </label>
                <input
                  type="tel"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  placeholder="Ví dụ: 0987654321"
                  style={{
                    width: '100%',
                    padding: '12px',
                    borderRadius: '8px',
                    border: '1px solid #cbd5e1'
                  }}
                />
              </div>

              <div style={{ marginBottom: '24px' }}>
                <label style={{ display: 'block', fontWeight: 600, marginBottom: '6px', fontSize: '14px' }}>
                  Đường dẫn ảnh đại diện (Avatar URL)
                </label>
                <input
                  type="url"
                  value={avatarUrl}
                  onChange={(e) => setAvatarUrl(e.target.value)}
                  placeholder="https://example.com/avatar.jpg"
                  style={{
                    width: '100%',
                    padding: '12px',
                    borderRadius: '8px',
                    border: '1px solid #cbd5e1'
                  }}
                />
              </div>

              <button
                type="submit"
                className="btn btn-primary"
                disabled={isSavingProfile}
                style={{ minWidth: '160px' }}
              >
                {isSavingProfile ? 'Đang lưu...' : 'Lưu thay đổi'}
              </button>
            </form>
          </div>
        )}

        {/* Nội dung Tab 2: Đổi mật khẩu */}
        {activeTab === 'password' && (
          <div style={{
            background: '#ffffff',
            padding: '28px',
            borderRadius: '12px',
            border: '1px solid #e8ecef'
          }}>
            {passwordMsg && (
              <div style={{
                background: passwordMsg.type === 'success' ? '#dcfce7' : '#fee2e2',
                color: passwordMsg.type === 'success' ? '#166534' : '#b91c1c',
                padding: '12px 16px',
                borderRadius: '8px',
                marginBottom: '20px',
                fontSize: '14px',
                border: `1px solid ${passwordMsg.type === 'success' ? '#86efac' : '#f87171'}`
              }}>
                {passwordMsg.text}
              </div>
            )}

            <form onSubmit={handleChangePassword}>
              <div style={{ marginBottom: '18px' }}>
                <label style={{ display: 'block', fontWeight: 600, marginBottom: '6px', fontSize: '14px' }}>
                  Mật khẩu hiện tại *
                </label>
                <input
                  type="password"
                  value={oldPassword}
                  onChange={(e) => setOldPassword(e.target.value)}
                  placeholder="Nhập mật khẩu hiện tại"
                  required
                  style={{
                    width: '100%',
                    padding: '12px',
                    borderRadius: '8px',
                    border: '1px solid #cbd5e1'
                  }}
                />
              </div>

              <div style={{ marginBottom: '18px' }}>
                <label style={{ display: 'block', fontWeight: 600, marginBottom: '6px', fontSize: '14px' }}>
                  Mật khẩu mới *
                </label>
                <input
                  type="password"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  placeholder="Tối thiểu 6 ký tự"
                  required
                  style={{
                    width: '100%',
                    padding: '12px',
                    borderRadius: '8px',
                    border: '1px solid #cbd5e1'
                  }}
                />
              </div>

              <div style={{ marginBottom: '24px' }}>
                <label style={{ display: 'block', fontWeight: 600, marginBottom: '6px', fontSize: '14px' }}>
                  Xác nhận mật khẩu mới *
                </label>
                <input
                  type="password"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  placeholder="Nhập lại mật khẩu mới"
                  required
                  style={{
                    width: '100%',
                    padding: '12px',
                    borderRadius: '8px',
                    border: '1px solid #cbd5e1'
                  }}
                />
              </div>

              <button
                type="submit"
                className="btn btn-primary"
                disabled={isSavingPassword}
                style={{ minWidth: '160px' }}
              >
                {isSavingPassword ? 'Đang đổi mật khẩu...' : 'Cập nhật mật khẩu'}
              </button>
            </form>
          </div>
        )}
      </div>
    </main>
  );
}
