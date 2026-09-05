import { Heart, MapPin, Maximize2 } from "lucide-react";
import { Link } from "react-router-dom";
import type { RoomDetail, RoomSummary } from "../types/room";

interface RoomCardProps {
  room: RoomSummary | RoomDetail;
}

export default function RoomCard({ room }: RoomCardProps) {
  const imageUrl =
    (room as RoomSummary).primaryImageUrl ||
    ((room as RoomDetail).images && (room as RoomDetail).images.length > 0
      ? (room as RoomDetail).images.find((img) => img.isPrimary)?.imageUrl || (room as RoomDetail).images[0].imageUrl
      : "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=800&q=80");

  const locationText = `${room.district}, ${room.city || "Hà Nội"}`;

  return (
    <article className="room-card">
      <div className="room-image-wrapper">
        <img src={imageUrl} alt={room.title} className="room-image" loading="lazy" />

        <button className="favorite-button" type="button" aria-label="Yêu thích">
          <Heart size={20} />
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