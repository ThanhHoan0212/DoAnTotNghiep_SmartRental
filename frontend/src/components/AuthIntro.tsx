import { Building2, MapPin, MessageCircle, FileCheck2, ArrowUpRight } from 'lucide-react';
import { Link } from 'react-router-dom';

export default function AuthIntro() {
  return <aside className="auth-intro">
    <Link to="/" className="auth-intro-brand"><Building2 size={24} /> SmartRental</Link>
    <div><span className="section-label">CHỌN NƠI Ở. BẮT ĐẦU HÀNH TRÌNH.</span>
      <h2>Không gian phù hợp.<br />Cuộc sống dễ dàng hơn.</h2>
      <p>Từ tìm phòng đến trao đổi với chủ nhà, mọi thứ bạn cần ở cùng một nơi.</p>
      <ul><li><MapPin size={19} /><span>Tìm phòng theo khu vực và ngân sách</span></li>
        <li><MessageCircle size={19} /><span>Nhắn tin cho chủ nhà từ tin phòng</span></li>
        <li><FileCheck2 size={19} /><span>Theo dõi hợp đồng và yêu cầu hỗ trợ</span></li></ul>
    </div>
    <Link to="/rooms" className="auth-intro-link">Xem phòng trước khi đăng nhập <ArrowUpRight size={17} /></Link>
  </aside>;
}
