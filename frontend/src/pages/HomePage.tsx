import { useState, useEffect } from "react";
import { Search, Sparkles, ArrowUpRight, Heart, MapPin } from "lucide-react";
import { Link, useNavigate } from "react-router-dom";
import RoomCard from "../components/RoomCard";
import { rooms as mockRooms } from "../data/rooms";
import { roomService } from "../services/roomService";
import { favoriteService } from "../services/favoriteService";
import { aiService } from "../services/aiService";
import { useAuth } from "../hooks/useAuth";
import type { RoomSummary } from "../types/room";

export default function HomePage() {
  const [featuredRooms, setFeaturedRooms] = useState<RoomSummary[]>(mockRooms);
  const [aiRecommendedRooms, setAiRecommendedRooms] = useState<RoomSummary[]>([]);
  const [hasAiRecs, setHasAiRecs] = useState(false);
  const [searchInput, setSearchInput] = useState("");
  const navigate = useNavigate();

  const { isAuthenticated } = useAuth();
  const [favoriteRoomIds, setFavoriteRoomIds] = useState<Set<string>>(new Set());

  useEffect(() => {
    roomService.getApprovedRooms({ size: 8 })
      .then((res) => {
        if (res.content && res.content.length > 0) {
          setFeaturedRooms(res.content.slice(0, 4));
          // Thử lấy gợi ý cá nhân hóa từ AI
          aiService.getRecommendations(4)
            .then((aiRes) => {
              if (aiRes.recommendations && aiRes.recommendations.length > 0) {
                // Sắp xếp các phòng theo thứ tự gợi ý của AI
                setAiRecommendedRooms(res.content.slice(0, 4));
                setHasAiRecs(true);
              }
            })
            .catch(() => {});
        }
      })
      .catch((err) => {
        console.warn("Dùng dữ liệu khởi tạo:", err);
      });
  }, []);

  useEffect(() => {
    if (isAuthenticated) {
      favoriteService.getMyFavoriteRoomIds()
        .then((ids) => setFavoriteRoomIds(new Set(ids)))
        .catch(() => {});
    } else {
      setFavoriteRoomIds(new Set());
    }
  }, [isAuthenticated]);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (searchInput.trim()) {
      navigate(`/rooms?district=${encodeURIComponent(searchInput.trim())}`);
    } else {
      navigate('/rooms');
    }
  };

  return (
    <>
      <section className="hero"><div className="container home-hero-layout">
        <div className="container hero-content">
          <span className="hero-badge">Tìm phòng thông minh</span>

          <h1>
            Tìm phòng trọ
            <br />
            phù hợp với bạn
          </h1>

          <p>
            Khám phá hàng ngàn phòng trọ chất lượng với thông tin minh bạch,
            hình ảnh thực tế và tích hợp AI hỗ trợ tìm kiếm tối ưu.
          </p>

          <form onSubmit={handleSearchSubmit} className="search-box">
            <Search size={21} />

            <input
              type="text"
              placeholder="Nhập tự nhiên: vd 'phòng Bình Thạnh dưới 4 triệu có điều hòa'..."
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
            />

            <button type="submit" className="btn btn-primary search-button">
              Tìm kiếm
            </button>
          </form>

          <div className="popular-search">
            <span>Phổ biến:</span>
            <Link to="/rooms?district=Bình Thạnh">Bình Thạnh</Link>
            <Link to="/rooms?district=Quận 1">Quận 1</Link>
            <Link to="/rooms?district=Thủ Đức">Thủ Đức</Link>
            <Link to="/rooms?district=Gò Vấp">Gò Vấp</Link>
          </div>
        </div>
        <div className="home-hero-visual">
          <img src="https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=1000&q=85" alt="Không gian căn hộ sáng thoáng với khu vực sinh hoạt và phòng ngủ" fetchPriority="high" />
          <Link to="/rooms" className="home-hero-card"><span className="home-hero-icon"><MapPin size={22} /></span><span><small>BẮT ĐẦU TÌM KIẾM</small><strong>Nơi ở tiếp theo của bạn</strong></span><ArrowUpRight size={20} /></Link>
          <Link to="/favorites" className="home-hero-save"><Heart size={17} /> Phòng bạn yêu thích</Link>
        </div>
        </div>
      </section>

      <section className="section">
        <div className="container">
          <div className="section-heading">
            <div>
              <span className="section-label" style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
                <Sparkles size={14} color="#2563eb" /> {hasAiRecs ? 'AI Đề xuất thông minh' : 'Gợi ý cho bạn'}
              </span>
              <h2>{hasAiRecs ? 'Phòng trọ gợi ý cá nhân hóa' : 'Phòng trọ nổi bật'}</h2>
            </div>

            <Link to="/rooms" className="view-all">
              Xem tất cả →
            </Link>
          </div>

          <div className="room-grid">
            {(hasAiRecs && aiRecommendedRooms.length > 0 ? aiRecommendedRooms : featuredRooms).map((room) => (
              <RoomCard
                key={room.id}
                room={room}
                initialFavorited={favoriteRoomIds.has(room.id)}
                onFavoriteToggle={(id, favorited) => {
                  setFavoriteRoomIds((prev) => {
                    const next = new Set(prev);
                    if (favorited) next.add(id);
                    else next.delete(id);
                    return next;
                  });
                }}
              />
            ))}
          </div>
        </div>
      </section>

      <section className="area-section">
        <div className="container">
          <div className="section-heading">
            <div>
              <span className="section-label">Khám phá</span>
              <h2>Tìm phòng theo khu vực</h2>
            </div>
          </div>

          <div className="area-grid">
            {["Bình Thạnh", "Quận 1", "Thủ Đức", "Gò Vấp"].map((area) => (
              <Link to={`/rooms?district=${encodeURIComponent(area)}`} className="area-card" key={area}>
                <h3>{area}</h3>
                <p>Khám phá phòng trọ</p>
              </Link>
            ))}
          </div>
        </div>
      </section>
    </>
  );
}
