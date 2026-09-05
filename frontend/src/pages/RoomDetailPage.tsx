import { useState, useEffect } from "react";
import { ArrowLeft, Check, Eye, Heart, MapPin, Maximize2, Phone, Mail } from "lucide-react";
import { Link, useParams } from "react-router-dom";
import { roomService } from "../services/roomService";
import type { RoomDetail } from "../types/room";

export default function RoomDetailPage() {
  const { id } = useParams<{ id: string }>();
  const [room, setRoom] = useState<RoomDetail | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedImage, setSelectedImage] = useState<string>("");

  useEffect(() => {
    if (!id) return;

    setIsLoading(true);
    roomService.getRoomDetail(id)
      .then((data) => {
        setRoom(data);
        if (data.images && data.images.length > 0) {
          const primary = data.images.find((img) => img.isPrimary) || data.images[0];
          setSelectedImage(primary.imageUrl);
        } else {
          setSelectedImage("https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=800&q=80");
        }
      })
      .catch((err) => {
        console.error("Lỗi tải chi tiết phòng:", err);
        setError("Không tìm thấy thông tin phòng trọ hoặc tin đăng đã bị gỡ.");
      })
      .finally(() => {
        setIsLoading(false);
      });
  }, [id]);

  if (isLoading) {
    return (
      <main className="page">
        <div className="container" style={{ textAlign: 'center', padding: '100px 0' }}>
          <h2>Đang tải chi tiết phòng trọ...</h2>
        </div>
      </main>
    );
  }

  if (error || !room) {
    return (
      <main className="page">
        <div className="container empty-state" style={{ textAlign: 'center', padding: '100px 0' }}>
          <h2>{error || "Không tìm thấy phòng"}</h2>
          <p style={{ margin: '16px 0' }}>
            <Link to="/rooms" className="btn btn-primary">
              Quay lại danh sách phòng
            </Link>
          </p>
        </div>
      </main>
    );
  }

  const fullAddress = `${room.address}${room.ward ? `, ${room.ward}` : ""}, ${room.district}, ${room.city}`;

  return (
    <main className="page">
      <div className="container">
        <Link to="/rooms" className="back-link">
          <ArrowLeft size={18} />
          Quay lại danh sách
        </Link>

        {/* Khung ảnh chính */}
        <div className="detail-image" style={{ borderRadius: '12px', overflow: 'hidden', height: '420px', background: '#000' }}>
          <img
            src={selectedImage}
            alt={room.title}
            style={{ width: '100%', height: '100%', objectFit: 'cover' }}
          />
        </div>

        {/* Gallery thumbnails nếu có nhiều hơn 1 ảnh */}
        {room.images && room.images.length > 1 && (
          <div style={{ display: 'flex', gap: '10px', marginTop: '12px', overflowX: 'auto', paddingBottom: '8px' }}>
            {room.images.map((img) => (
              <button
                key={img.id}
                type="button"
                onClick={() => setSelectedImage(img.imageUrl)}
                style={{
                  width: '80px',
                  height: '60px',
                  borderRadius: '6px',
                  overflow: 'hidden',
                  border: selectedImage === img.imageUrl ? '2px solid #167c5a' : '2px solid transparent',
                  padding: 0,
                  cursor: 'pointer',
                  flexShrink: 0
                }}
              >
                <img src={img.imageUrl} alt="" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
              </button>
            ))}
          </div>
        )}

        <div className="detail-layout" style={{ marginTop: '24px' }}>
          <div className="detail-main">
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '8px' }}>
              <span className="verified-badge" style={{ background: '#dcfce7', color: '#166534', padding: '4px 10px', borderRadius: '12px', fontSize: '13px', fontWeight: 600 }}>
                ✓ Tin đăng đã kiểm duyệt
              </span>

              <span style={{ fontSize: '13px', color: '#64748b', display: 'flex', alignItems: 'center', gap: '4px' }}>
                <Eye size={16} /> {room.viewCount} lượt xem
              </span>
            </div>

            <h1>{room.title}</h1>

            <p className="detail-price" style={{ fontSize: '28px', fontWeight: 700, color: '#167c5a' }}>
              {room.price ? room.price.toLocaleString("vi-VN") : 0} đ
              <span style={{ fontSize: '16px', color: '#64748b', fontWeight: 400 }}> / tháng</span>
            </p>

            <div className="detail-meta">
              <span>
                <MapPin size={19} />
                {fullAddress}
              </span>

              <span>
                <Maximize2 size={19} />
                {room.area} m²
              </span>
            </div>

            <hr style={{ border: 'none', borderTop: '1px solid #e2e8f0', margin: '24px 0' }} />

            <h2>Mô tả phòng</h2>
            <p className="description" style={{ whiteSpace: 'pre-line', lineHeight: '1.7', color: '#334155' }}>
              {room.description || "Chủ nhà chưa cập nhật mô tả chi tiết cho phòng trọ này."}
            </p>

            <hr style={{ border: 'none', borderTop: '1px solid #e2e8f0', margin: '24px 0' }} />

            <h2>Tiện ích có sẵn ({room.amenities ? room.amenities.length : 0})</h2>

            <div className="detail-amenities" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '12px', marginTop: '16px' }}>
              {room.amenities && room.amenities.length > 0 ? (
                room.amenities.map((item) => (
                  <span key={item.id} style={{ display: 'flex', alignItems: 'center', gap: '8px', padding: '8px 12px', background: '#f8fafc', borderRadius: '8px', border: '1px solid #e2e8f0' }}>
                    <Check size={17} color="#167c5a" />
                    {item.name}
                  </span>
                ))
              ) : (
                <p style={{ color: '#64748b' }}>Chưa có thông tin tiện ích đính kèm.</p>
              )}
            </div>
          </div>

          <aside className="contact-card">
            <p style={{ fontWeight: 600, color: '#64748b', marginBottom: '12px' }}>Thông tin người cho thuê</p>

            <div className="owner" style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '20px' }}>
              <div className="avatar" style={{
                width: '48px',
                height: '48px',
                borderRadius: '50%',
                background: room.landlord?.avatarUrl ? `url(${room.landlord.avatarUrl}) center/cover` : '#167c5a',
                color: '#fff',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontWeight: 700
              }}>
                {!room.landlord?.avatarUrl && (room.landlord?.fullName ? room.landlord.fullName[0].toUpperCase() : 'C')}
              </div>

              <div>
                <strong>{room.landlord?.fullName || "Chủ nhà SmartRental"}</strong>
                <div style={{ fontSize: '12px', color: '#64748b' }}>
                  {room.landlord?.role === 'ADMIN' ? 'Ban quản trị' : 'Chủ nhà xác thực'}
                </div>
              </div>
            </div>

            {room.landlord?.phone && (
              <a
                href={`tel:${room.landlord.phone}`}
                className="btn btn-primary btn-full"
                style={{ textDecoration: 'none', marginBottom: '10px' }}
              >
                <Phone size={18} />
                Gọi điện: {room.landlord.phone}
              </a>
            )}

            {room.landlord?.email && (
              <a
                href={`mailto:${room.landlord.email}`}
                className="btn btn-outline btn-full"
                style={{ textDecoration: 'none', marginBottom: '10px', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px' }}
              >
                <Mail size={18} />
                Gửi Email
              </a>
            )}

            <button
              className="btn btn-outline btn-full"
              type="button"
              onClick={() => alert("Đã lưu phòng vào danh sách quan tâm!")}
              style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px' }}
            >
              <Heart size={18} />
              Lưu tin đăng
            </button>
          </aside>
        </div>
      </div>
    </main>
  );
}