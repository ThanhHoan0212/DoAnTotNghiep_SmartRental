import { useState, useEffect } from "react";
import { Heart, MapPin, Maximize2 } from "lucide-react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../hooks/useAuth";
import { favoriteService } from "../services/favoriteService";
import type { RoomDetail, RoomSummary } from "../types/room";

interface RoomCardProps {
  room: RoomSummary | RoomDetail;
  initialFavorited?: boolean;
  onFavoriteToggle?: (roomId: string, isFavorited: boolean) => void;
}

export default function RoomCard({ room, initialFavorited = false, onFavoriteToggle }: RoomCardProps) {
  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const [isFavorited, setIsFavorited] = useState<boolean>(initialFavorited);
  const [isToggling, setIsToggling] = useState<boolean>(false);

  // Cập nhật trạng thái khi prop initialFavorited thay đổi
  useEffect(() => {
    setIsFavorited(Boolean(initialFavorited));
  }, [initialFavorited]);

  const imageUrl =
    (room as RoomSummary).primaryImageUrl ||
    ((room as RoomDetail).images && (room as RoomDetail).images.length > 0
      ? (room as RoomDetail).images.find((img) => img.isPrimary)?.imageUrl || (room as RoomDetail).images[0].imageUrl
      : "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=800&q=80");

  const locationText = `${room.district}, ${room.city || "TP. Hồ Chí Minh"}`;

  const handleFavoriteClick = async (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();

    if (!isAuthenticated) {
      if (window.confirm("Vui lòng đăng nhập để lưu phòng trọ vào danh sách yêu thích. Bạn có muốn chuyển đến trang đăng nhập ngay không?")) {
        navigate("/login");
      }
      return;
    }

    if (isToggling) return;

    // Optimistic UI update
    const previousState = isFavorited;
    const nextState = !previousState;
    setIsFavorited(nextState);
    setIsToggling(true);

    try {
      const res = await favoriteService.toggleFavorite(room.id);
      // Hỗ trợ cả 2 định dạng Jackson: isFavorited và favorited
      const newStatus =
        res.isFavorited !== undefined
          ? res.isFavorited
          : res.favorited !== undefined
          ? res.favorited
          : nextState;
      setIsFavorited(Boolean(newStatus));
      if (onFavoriteToggle) {
        onFavoriteToggle(room.id, Boolean(newStatus));
      }
    } catch (error: any) {
      console.error("Lỗi khi cập nhật yêu thích:", error);
      setIsFavorited(previousState);
      const msg = error?.message || "Không thể cập nhật trạng thái yêu thích. Vui lòng thử lại!";
      alert(msg);
    } finally {
      setIsToggling(false);
    }
  };

  return (
    <article className="room-card">
      <div className="room-image-wrapper">
        <img src={imageUrl} alt={room.title} className="room-image" loading="lazy" />

        <button
          className={`favorite-button ${isFavorited ? "favorited" : ""}`}
          type="button"
          onClick={handleFavoriteClick}
          aria-label={isFavorited ? "Bỏ lưu phòng trọ" : "Lưu vào yêu thích"}
          title={isFavorited ? "Bỏ lưu yêu thích" : "Lưu vào yêu thích"}
          disabled={isToggling}
        >
          <Heart
            size={19}
            color={isFavorited ? "#ef4444" : "#475569"}
            fill={isFavorited ? "#ef4444" : "transparent"}
            className={isFavorited ? "heart-active" : ""}
          />
        </button>
      </div>

      <div className="room-card-body">
        <Link to={`/rooms/${room.id}`} className="room-title">
          {room.title}
        </Link>

        <p className="room-price">
          {room.price ? room.price.toLocaleString("vi-VN") : 0} đ/tháng
        </p>

        <div className="room-info">
          <span>
            <MapPin size={16} />
            {locationText}
          </span>

          <span>
            <Maximize2 size={16} />
            {room.area} m²
          </span>
        </div>

        {"amenities" in room && room.amenities && room.amenities.length > 0 && (
          <div className="amenities">
            {room.amenities.slice(0, 3).map((amenity) => (
              <span key={typeof amenity === "string" ? amenity : amenity.id}>
                {typeof amenity === "string" ? amenity : amenity.name}
              </span>
            ))}
          </div>
        )}
      </div>
    </article>
  );
}