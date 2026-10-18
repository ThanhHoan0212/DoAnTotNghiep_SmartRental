import { NavLink, Route, Routes } from 'react-router-dom';
import { LayoutDashboard, Building2, Users, MessageSquareWarning, ChartNoAxesCombined, ShieldCheck } from 'lucide-react';
import { useAuth } from '../hooks/useAuth';
import AdminRooms from './admin/AdminRooms';
import AdminUsers from './admin/AdminUsers';
import AdminComplaints from './admin/AdminComplaints';

import AdminAnalytics from './admin/AdminAnalytics';
import './AdminPage.css';

export default function AdminPage() {
  const { user } = useAuth();
  return <main className="admin-shell"><aside className="admin-sidebar"><div className="admin-brand"><ShieldCheck size={24} /><div><strong>SmartRental</strong><small>TRUNG TÂM QUẢN TRỊ</small></div></div>
    <p className="admin-nav-label">QUẢN LÝ HỆ THỐNG</p><nav aria-label="Điều hướng quản trị">
      {[['/admin', 'Tổng quan', LayoutDashboard], ['/admin/rooms', 'Phòng cho thuê', Building2], ['/admin/users', 'Tài khoản', Users], ['/admin/complaints', 'Xử lý khiếu nại', MessageSquareWarning], ['/admin/reports', 'Báo cáo & phân tích', ChartNoAxesCombined]].map(([path, label, Icon]) => {
        const ItemIcon = Icon as typeof LayoutDashboard;
        return <NavLink key={String(path)} end={path === '/admin'} to={String(path)}><ItemIcon size={19} />{String(label)}</NavLink>;
      })}
    </nav><div className="admin-sidebar-user"><span className="admin-avatar">{user?.fullName?.charAt(0)}</span><div><strong>{user?.fullName}</strong><small>Quản trị viên hệ thống</small></div></div>
  </aside><div className="admin-main"><Routes><Route index element={<AdminAnalytics overview />} /><Route path="rooms" element={<AdminRooms />} /><Route path="users" element={<AdminUsers />} /><Route path="complaints" element={<AdminComplaints />} /><Route path="reports" element={<AdminAnalytics />} /><Route path="*" element={<p>Không tìm thấy trang quản trị.</p>} /></Routes></div></main>;
}
