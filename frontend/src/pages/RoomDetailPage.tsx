import { useState, useEffect } from "react";
import { ArrowLeft, Check, Eye, Heart, MapPin, Maximize2, Phone, Mail, FileText, CheckCircle } from "lucide-react";
import { Link, useParams, useNavigate } from "react-router-dom";
import { roomService } from "../services/roomService";
import { favoriteService } from "../services/favoriteService";
import { useAuth } from "../context/AuthContext";
import { RentalRequestModal } from "../components/RentalRequestModal";
import type { RoomDetail } from "../types/room";
import type { Contract } from "../types/contract";

export default function RoomDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { user, isAuthenticated } = useAuth();

  const [room, setRoom] = useState<RoomDetail | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedImage, setSelectedImage] = useState<string>("");

  // Trạng thái yêu thích
  const [isFavorited, setIsFavorited] = useState<boolean>(false);
  const [totalFavorites, setTotalFavorites] = useState<number>(0);
  const [isTogglingFav, setIsTogglingFav] = useState<boolean>(false);

  // Trạng thái modal đặt cọc / hợp đồng
  const [isRentalModalOpen, setIsRentalModalOpen] = useState<boolean>(false);
  const [createdContract, setCreatedContract] = useState<Contract | null>(null);

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

    // Tải trạng thái yêu thích nếu có id
    favoriteService.checkFavoriteStatus(id)
      .then((status) => {
        setIsFavorited(status.isFavorited || status.favorited || false);
        setTotalFavorites(status.totalFavorites || 0);
      })
      .catch((err) => {
        console.warn("Không thể kiểm tra trạng thái yêu thích:", err);
      });
  }, [id]);

  const handleToggleFavorite = async () => {
    if (!room) return;
    if (!isAuthenticated) {
      alert("Vui lòng đăng nhập để lưu phòng trọ vào danh sách quan tâm!");
      navigate("/login");
      return;
    }

    try {
      setIsTogglingFav(true);
      const res = await favoriteService.toggleFavorite(room.id);
      const nextState = res.isFavorited !== undefined ? res.isFavorited : (res.favorited ?? !isFavorited);
      setIsFavorited(nextState);
      setTotalFavorites(res.totalFavorites);
    } catch (err) {
      console.error("Lỗi lưu yêu thích:", err);
    } finally {
      setIsTogglingFav(false);
    }
  };

  const handleOpenRentalModal = () => {
    if (!isAuthenticated) {
      alert("Vui lòng đăng nhập để gửi yêu cầu thuê phòng!");
      navigate("/login");
      return;
    }
    setIsRentalModalOpen(true);
  };

  const handleRentalSuccess = (contract: Contract) => {
    setIsRentalModalOpen(false);
    setCreatedContract(contract);
  };

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
  const isOwner = user?.id === room.landlord?.id;
  const isRented = room.status === "RENTED";

  return (
    <main className="page">
      <div className="container">
        <Link to="/rooms" className="back-link">
          <ArrowLeft size={18} />
          Quay lại danh sách
        </Link>

        {/* Thông báo gửi yêu cầu thuê phòng thành công */}
        {createdContract && (
          <div
            style={{
              background: '#ecfdf5',
              border: '1px solid #a7f3d0',
              borderRadius: '12px',
              padding: '16px 20px',
              marginBottom: '20px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              gap: '16px',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
              <CheckCircle size={24} color="#10b981" />
              <div>
                <strong style={{ color: '#065f46', fontSize: '15px' }}>
                  Gửi yêu cầu thuê phòng thành công! (Mã HĐ: {createdContract.contractCode || createdContract.contractNumber || createdContract.id?.slice(0, 8)})
                </strong>
                <p style={{ margin: '4px 0 0', fontSize: '13px', color: '#047857' }}>
                  Chủ nhà sẽ nhận được thông báo để xét duyệt yêu cầu của bạn. Bạn có thể theo dõi tiến độ trong mục Quản lý hợp đồng.
                </p>
              </div>
            </div>
            <Link to="/contracts" className="btn btn-primary" style={{ whiteSpace: 'nowrap', padding: '8px 16px', fontSize: '13px' }}>
              Xem hợp đồng của tôi
            </Link>
          </div>
        )}

        {/* Khung ảnh chính */}
        <div className="detail-image" style={{ borderRadius: '12px', overflow: 'hidden', height: '420px', background: '#000', position: 'relative' }}>
          <img
            src={selectedImage}
            alt={room.title}
            style={{ width: '100%', height: '100%', objectFit: 'cover' }}
          />

          {isRented && (
            <div
              style={{
                position: 'absolute',
                top: '16px',
                right: '16px',
                background: '#dc2626',
                color: '#fff',
                padding: '8px 16px',
                borderRadius: '8px',
                fontWeight: 700,
                fontSize: '14px',
                boxShadow: '0 4px 6px rgba(0,0,0,0.2)',
              }}
            >
              ĐÃ CÓ NGƯỜI THUÊ
            </div>
          )}
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
                  flexShrink: 0,
                }}
              >
                <img src={img.imageUrl} alt="" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
              </button>
            ))}
          </div>
        )}

        <div className="detail-layout" style={{ marginTop: '24px' }}>
          <div className="detail-main">
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '8px', flexWrap: 'wrap' }}>
              {isRented ? (
                <span className="status-badge status-terminated">
                  ● Đã cho thuê
                </span>
              ) : (
                <span className="verified-badge" style={{ background: '#dcfce7', color: '#166534', padding: '4px 10px', borderRadius: '12px', fontSize: '13px', fontWeight: 600 }}>
                  ✓ Tin đăng đã kiểm duyệt
                </span>
              )}

              <span style={{ fontSize: '13px', color: '#64748b', display: 'flex', alignItems: 'center', gap: '4px' }}>
                <Eye size={16} /> {room.viewCount} lượt xem
              </span>

              {totalFavorites > 0 && (
                <span style={{ fontSize: '13px', color: '#e11d48', display: 'flex', alignItems: 'center', gap: '4px' }}>
                  <Heart size={15} fill="#e11d48" /> {totalFavorites} người quan tâm
                </span>
              )}
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
                fontWeight: 700,
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

            {/* NÚT THUÊ PHÒNG / ĐẶT CỌC */}
            {isOwner ? (
              <div
                style={{
                  background: '#f1f5f9',
                  color: '#475569',
                  padding: '12px',
                  borderRadius: '8px',
                  fontSize: '13px',
                  textAlign: 'center',
                  marginBottom: '12px',
                  fontWeight: 500,
                }}
              >
                Bạn là chủ sở hữu của phòng trọ này
              </div>
            ) : isRented ? (
              <button
                type="button"
                className="btn btn-full"
                disabled
                style={{
                  background: '#f1f5f9',
                  color: '#94a3b8',
                  cursor: 'not-allowed',
                  marginBottom: '12px',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: '8px',
                }}
              >
                <FileText size={18} />
                Phòng đã có người thuê
              </button>
            ) : (
              <button
                type="button"
                className="btn btn-primary btn-full"
                onClick={handleOpenRentalModal}
                style={{
                  marginBottom: '12px',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: '8px',
                  background: '#167c5a',
                  fontWeight: 600,
                }}
              >
                <FileText size={18} />
                Gửi yêu cầu thuê phòng
              </button>
            )}

            {room.landlord?.phone && (
              <a
                href={`tel:${room.landlord.phone}`}
                className="btn btn-outline btn-full"
                style={{ textDecoration: 'none', marginBottom: '10px', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px' }}
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
              onClick={handleToggleFavorite}
              disabled={isTogglingFav}
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '8px',
                borderColor: isFavorited ? '#e11d48' : undefined,
                color: isFavorited ? '#e11d48' : undefined,
                background: isFavorited ? '#fff1f2' : undefined,
              }}
            >
              <Heart
                size={18}
                color={isFavorited ? '#e11d48' : 'currentColor'}
                fill={isFavorited ? '#e11d48' : 'none'}
              />
              {isFavorited ? "Đã quan tâm tin này" : "Lưu tin đăng"}
            </button>
          </aside>
        </div>
      </div>

      {/* Modal gửi yêu cầu thuê phòng */}
      <RentalRequestModal
        room={room}
        isOpen={isRentalModalOpen}
        onClose={() => setIsRentalModalOpen(false)}
        onSuccess={handleRentalSuccess}
      />
    </main>
  );
}