import { useState, useRef, useEffect } from "react";
import { Link, NavLink, useNavigate } from "react-router-dom";
import { useAuth } from "../hooks/useAuth";
import { Building2, ChevronDown, ShieldCheck } from "lucide-react";

export default function Header() {
  const { user, isAuthenticated, logout } = useAuth();
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);
  const navigate = useNavigate();

  // Đóng dropdown khi click ra ngoài
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setDropdownOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, []);

  const handleLogout = async () => {
    setDropdownOpen(false);
    await logout();
    navigate("/login");
  };

  const getRoleBadge = (role?: string) => {
    switch (role) {
      case "ADMIN":
        return { label: "Admin", bg: "#fee2e2", color: "#991b1b" };
      case "LANDLORD":
        return { label: "Chủ nhà", bg: "#e0f2fe", color: "#075985" };
      case "TENANT":
      default:
        return { label: "Người thuê", bg: "#dcfce7", color: "#166534" };
    }
  };

  const roleInfo = getRoleBadge(user?.role);
  const isLandlordOrAdmin = user?.role === "LANDLORD" || user?.role === "ADMIN";

  return (
    <header className="header">
      <div className="container header-content">
        <Link to="/" className="logo">
          <span className="logo-mark"><Building2 size={21} /></span>
          SmartRental
        </Link>

        <nav className="nav">
          <NavLink to="/" end>Trang chủ</NavLink>
          <NavLink to="/rooms">Phòng trọ</NavLink>
          {user?.role === 'ADMIN' && <NavLink to="/admin">Quản trị</NavLink>}
          <NavLink to="/favorites">Yêu thích</NavLink>
          {isAuthenticated && <NavLink to="/contracts">Hợp đồng</NavLink>}
          {isAuthenticated && <NavLink to="/chat">Tin nhắn</NavLink>}
        </nav>

        <div className="header-actions">
          {isLandlordOrAdmin && (
            <Link
              to="/post-room"
              className="btn btn-primary"
              style={{
                padding: "0 16px",
                fontSize: "14px",
                textDecoration: "none",
                borderRadius: "8px",
              }}
            >
              + Đăng tin
            </Link>
          )}

          {isAuthenticated && user ? (
            <div className="user-menu" ref={dropdownRef} style={{ position: "relative" }}>
              <button type="button" className="header-account" onClick={() => setDropdownOpen(!dropdownOpen)}
                aria-label={`Tài khoản ${user.fullName}`} aria-expanded={dropdownOpen} aria-controls="header-account-menu" title={user.fullName}>
                <span className="header-account-avatar" style={user.avatarUrl ? { backgroundImage: `url(${user.avatarUrl})` } : undefined}>
                  {!user.avatarUrl && (user.fullName?.[0]?.toUpperCase() || 'U')}
                </span>
                <span className="header-account-copy">
                  <strong>{user.fullName}</strong>
                  <span className="header-account-meta">{roleInfo.label}
                    {user.isIdentityVerified && <span className="header-account-verified"><ShieldCheck size={12} />Đã xác thực</span>}
                  </span>
                </span>
                <ChevronDown size={15} className={`header-account-chevron ${dropdownOpen ? 'open' : ''}`} />
              </button>
              {/* Dropdown Menu */}
              {dropdownOpen && (
                <div id="header-account-menu"
                  style={{
                    position: "absolute",
                    top: "100%",
                    right: 0,
                    marginTop: "8px",
                    width: "210px",
                    background: "#ffffff",
                    borderRadius: "10px",
                    boxShadow: "0 10px 25px rgba(0,0,0,0.1)",
                    border: "1px solid #e2e8f0",
                    overflow: "hidden",
                    zIndex: 200,
                  }}
                >
                  <div style={{ padding: "12px 16px", borderBottom: "1px solid #f1f5f9" }}>
                    <strong style={{ display: "block", fontSize: 14, marginBottom: 6, overflowWrap: "anywhere" }}>{user.fullName}</strong>
                    <div style={{ fontSize: "12px", color: "#64748b" }}>Đăng nhập với email</div>
                    <div style={{ fontSize: "13px", fontWeight: 600, overflow: "hidden", textOverflow: "ellipsis" }}>
                      {user.email}
                    </div>
                  </div>

                  {user.role === 'ADMIN' && <Link to="/admin" onClick={() => setDropdownOpen(false)} style={{ display: 'block', padding: '12px 16px', fontWeight: 700, color: '#167c5a' }}>Trung tâm quản trị</Link>}
                  <Link
                    to="/profile"
                    onClick={() => setDropdownOpen(false)}
                    style={{
                      display: "block",
                      padding: "10px 16px",
                      fontSize: "14px",
                      color: "#1b2430",
                      transition: "background 0.2s",
                    }}
                    onMouseEnter={(e) => (e.currentTarget.style.background = "#f8fafc")}
                    onMouseLeave={(e) => (e.currentTarget.style.background = "transparent")}
                  >
                    👤 Thông tin cá nhân
                  </Link>

                  <Link
                    to="/ekyc"
                    onClick={() => setDropdownOpen(false)}
                    style={{
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "space-between",
                      padding: "10px 16px",
                      fontSize: "14px",
                      color: user.isIdentityVerified ? "#059669" : "#b45309",
                      background: user.isIdentityVerified ? "#f0fdf4" : "#fffbeb",
                      fontWeight: 600,
                      borderTop: "1px solid #f1f5f9",
                      borderBottom: "1px solid #f1f5f9",
                      transition: "background 0.2s",
                    }}
                    onMouseEnter={(e) => (e.currentTarget.style.filter = "brightness(0.96)")}
                    onMouseLeave={(e) => (e.currentTarget.style.filter = "none")}
                  >
                    <span>🛡️ Định danh eKYC</span>
                    {user.isIdentityVerified ? (
                      <span style={{ fontSize: "11px", color: "#059669", fontWeight: 700 }}>✓ Đã duyệt</span>
                    ) : (
                      <span style={{ fontSize: "11px", color: "#d97706", fontWeight: 700 }}>Cần làm ⚠️</span>
                    )}
                  </Link>

                  <Link
                    to="/favorites"
                    onClick={() => setDropdownOpen(false)}
                    style={{
                      display: "block",
                      padding: "10px 16px",
                      fontSize: "14px",
                      color: "#1b2430",
                      transition: "background 0.2s",
                    }}
                    onMouseEnter={(e) => (e.currentTarget.style.background = "#f8fafc")}
                    onMouseLeave={(e) => (e.currentTarget.style.background = "transparent")}
                  >
                    ❤️ Tin đã lưu
                  </Link>

                  <Link
                    to="/contracts"
                    onClick={() => setDropdownOpen(false)}
                    style={{
                      display: "block",
                      padding: "10px 16px",
                      fontSize: "14px",
                      color: "#1b2430",
                      transition: "background 0.2s",
                    }}
                    onMouseEnter={(e) => (e.currentTarget.style.background = "#f8fafc")}
                    onMouseLeave={(e) => (e.currentTarget.style.background = "transparent")}
                  >
                    📝 Quản lý hợp đồng
                  </Link>

                  {isLandlordOrAdmin && (
                    <Link
                      to="/post-room"
                      onClick={() => setDropdownOpen(false)}
                      style={{
                        display: "block",
                        padding: "10px 16px",
                        fontSize: "14px",
                        color: "#167c5a",
                        fontWeight: 600,
                      }}
                      onMouseEnter={(e) => (e.currentTarget.style.background = "#f0fdf4")}
                      onMouseLeave={(e) => (e.currentTarget.style.background = "transparent")}
                    >
                      ➕ Đăng tin phòng mới
                    </Link>
                  )}

                  <button
                    type="button"
                    onClick={handleLogout}
                    style={{
                      width: "100%",
                      textAlign: "left",
                      padding: "10px 16px",
                      fontSize: "14px",
                      color: "#dc2626",
                      background: "none",
                      border: "none",
                      borderTop: "1px solid #f1f5f9",
                      cursor: "pointer",
                      fontWeight: 500,
                    }}
                    onMouseEnter={(e) => (e.currentTarget.style.background = "#fef2f2")}
                    onMouseLeave={(e) => (e.currentTarget.style.background = "transparent")}
                  >
                    🚪 Đăng xuất
                  </button>
                </div>
              )}
            </div>
          ) : (
            <>
              <Link to="/login" className="login-link">
                Đăng nhập
              </Link>

              <Link to="/register" className="btn btn-primary">
                Đăng ký
              </Link>
            </>
          )}
        </div>
      </div>
    </header>
  );
}
