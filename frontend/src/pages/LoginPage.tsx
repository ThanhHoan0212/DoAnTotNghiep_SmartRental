import React, { useState, useEffect } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { ApiError } from '../services/api';

export default function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [rememberMe, setRememberMe] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const { login, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const redirectPath = (location.state as any)?.from?.pathname || '/';

  useEffect(() => {
    if (isAuthenticated) {
      navigate(redirectPath, { replace: true });
    }
  }, [isAuthenticated, navigate, redirectPath]);

  useEffect(() => {
    const savedEmail = localStorage.getItem('savedEmail');
    if (savedEmail) {
      setEmail(savedEmail);
    }
  }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!email.trim() || !password) {
      setError('Vui lòng nhập đầy đủ email và mật khẩu');
      return;
    }

    setIsSubmitting(true);
    try {
      await login({ email: email.trim(), password });

      if (rememberMe) {
        localStorage.setItem('savedEmail', email.trim());
      } else {
        localStorage.removeItem('savedEmail');
      }

      navigate(redirectPath, { replace: true });
    } catch (err: any) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError(err?.message || 'Đăng nhập không thành công, vui lòng thử lại');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  // Nút điền nhanh tài khoản Admin để demo hội đồng
  const fillAdminAccount = () => {
    setEmail('admin@phongtro.vn');
    setPassword('Admin@123');
    setError(null);
  };

  return (
    <main className="auth-page">
      <div className="auth-card">
        <span className="section-label">Chào mừng trở lại</span>

        <h1>Đăng nhập</h1>

        <p>Đăng nhập để tiếp tục tìm phòng phù hợp với bạn.</p>

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
            Email
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
            Mật khẩu
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Nhập mật khẩu"
              required
              disabled={isSubmitting}
            />
          </label>

          <div className="form-options">
            <label className="remember">
              <input
                type="checkbox"
                checked={rememberMe}
                onChange={(e) => setRememberMe(e.target.checked)}
              />
              Ghi nhớ đăng nhập
            </label>

            <a
              href="#forgot"
              onClick={(e) => {
                e.preventDefault();
                alert('Tính năng khôi phục mật khẩu: Nhập email và dùng endpoint /api/v1/auth/forgot-password');
              }}
            >
              Quên mật khẩu?
            </a>
          </div>

          <button
            type="submit"
            className="btn btn-primary btn-full"
            disabled={isSubmitting}
            style={{ opacity: isSubmitting ? 0.7 : 1 }}
          >
            {isSubmitting ? 'Đang xử lý...' : 'Đăng nhập'}
          </button>
        </form>

        {/* Khối tiện ích demo đồ án */}
        <div style={{ marginTop: '16px', textAlign: 'center' }}>
          <button
            type="button"
            onClick={fillAdminAccount}
            style={{
              background: 'none',
              border: '1px dashed #167c5a',
              color: '#167c5a',
              padding: '6px 12px',
              borderRadius: '6px',
              fontSize: '12px',
              cursor: 'pointer',
              fontWeight: 500
            }}
          >
            ⚡ Demo: Điền tài khoản Admin (admin@phongtro.vn)
          </button>
        </div>

        <p className="auth-footer">
          Chưa có tài khoản? <Link to="/register">Đăng ký</Link>
        </p>
      </div>
    </main>
  );
}