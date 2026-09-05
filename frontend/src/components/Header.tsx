import { useState, useRef, useEffect } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../hooks/useAuth";

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
          SmartRental
        </Link>

        <nav className="nav">
          <Link to="/">Trang chủ</Link>
          <Link to="/rooms">Phòng trọ</Link>
          <Link to="/rooms">Tìm phòng</Link>
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
              <button
                type="button"
                onClick={() => setDropdownOpen(!dropdownOpen)}
                style={{
                  display: "flex",
                  alignItems: "center",
                  gap: "10px",
                  background: "#f8fafc",
                  border: "1px solid #e2e8f0",
                  padding: "6px 14px",
                  borderRadius: "24px",
                  cursor: "pointer",
                }}
              >
                <div
                  style={{
                    width: "32px",
                    height: "32px",
                    borderRadius: "50%",
                    background: user.avatarUrl ? `url(${user.avatarUrl}) center/cover` : "#167c5a",
                    color: "#ffffff",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    fontWeight: 700,
                    fontSize: "14px",
                  }}
                >
                  {!user.avatarUrl && (user.fullName ? user.fullName[0].toUpperCase() : "U")}
                </div>

                <div style={{ textAlign: "left" }}>
                  <div style={{ fontWeight: 600, fontSize: "14px", color: "#1b2430" }}>
                    {user.fullName}
                  </div>
                  <div
                    style={{
                      fontSize: "11px",
                      fontWeight: 600,
                      display: "inline-block",
                      padding: "1px 6px",
                      borderRadius: "6px",
                      background: roleInfo.bg,
                      color: roleInfo.color,
                    }}
                  >
                    {roleInfo.label}
                  </div>
                </div>

                <span style={{ fontSize: "10px", color: "#64748b", marginLeft: "4px" }}>▼</span>
              </button>

              {/* Dropdown Menu */}
              {dropdownOpen && (
                <div
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
                    <div style={{ fontSize: "12px", color: "#64748b" }}>Đăng nhập với email</div>
                    <div style={{ fontSize: "13px", fontWeight: 600, overflow: "hidden", textOverflow: "ellipsis" }}>
                      {user.email}
                    </div>
                  </div>

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