import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import type { Role } from '../types/auth';
import { ApiError } from '../services/api';

export default function RegisterPage() {
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [role, setRole] = useState<Role>('TENANT');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const { register } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!fullName.trim() || !email.trim() || !password) {
      setError('Vui lòng điền đầy đủ các trường thông tin bắt buộc (*)');
      return;
    }

    if (password.length < 6) {
      setError('Mật khẩu phải có tối thiểu 6 ký tự');
      return;
    }

    if (password !== confirmPassword) {
      setError('Mật khẩu xác nhận không khớp');
      return;
    }

    if (phone.trim() && !/^(0|\+84)[3|5|7|8|9][0-9]{8}$/.test(phone.trim())) {
      setError('Số điện thoại không đúng định dạng Việt Nam (10 số)');
      return;
    }

    setIsSubmitting(true);
    try {
      await register({
        fullName: fullName.trim(),
        email: email.trim(),
        phone: phone.trim() ? phone.trim() : undefined,
        role,
        password,
      });

      // Đăng ký thành công, chuyển hướng về trang chủ
      navigate('/', { replace: true });
    } catch (err: any) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError(err?.message || 'Đăng ký không thành công, vui lòng thử lại');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <main className="auth-page">
      <div className="auth-card">
        <span className="section-label">Bắt đầu</span>

        <h1>Tạo tài khoản</h1>

        <p>Đăng ký tài khoản SmartRental để trải nghiệm tìm phòng thông minh.</p>

        {error && (
          <div className="alert alert-error" style={{
            background: '#fee2e2',
            color: '#b91c1c',
            padding: '10px 14px',
            borderRadius: '6px',
            marginBottom: '16px',
            fontSize: '14px',
            border: '1px solid #f87171'
          }}>
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <label>
            Họ và tên *
            <input
              type="text"
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
              placeholder="Nguyễn Văn A"
              required
              disabled={isSubmitting}
            />
          </label>

          <label>
            Email *
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="example@gmail.com"
              required
              disabled={isSubmitting}
            />
          </label>

          <label>
            Số điện thoại
            <input
              type="tel"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              placeholder="0987654321"
              disabled={isSubmitting}
            />
          </label>

          <label>
            Bạn là
            <select
              value={role}
              onChange={(e) => setRole(e.target.value as Role)}
              disabled={isSubmitting}
            >
              <option value="TENANT">Người thuê phòng</option>
              <option value="LANDLORD">Chủ nhà / Cho thuê</option>
            </select>
          </label>

          <label>
            Mật khẩu *
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Tối thiểu 6 ký tự"
              required
              disabled={isSubmitting}
            />
          </label>

          <label>
            Xác nhận mật khẩu *
            <input
              type="password"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              placeholder="Nhập lại mật khẩu"
              required
              disabled={isSubmitting}
            />
          </label>

          <button
            type="submit"
            className="btn btn-primary btn-full"
            disabled={isSubmitting}
            style={{ opacity: isSubmitting ? 0.7 : 1 }}
          >
            {isSubmitting ? 'Đang khởi tạo tài khoản...' : 'Đăng ký'}
          </button>
        </form>

        <p className="auth-footer">
          Đã có tài khoản? <Link to="/login">Đăng nhập</Link>
        </p>
      </div>
    </main>
  );
}