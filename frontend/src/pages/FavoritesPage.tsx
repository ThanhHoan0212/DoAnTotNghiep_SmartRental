import { useState, useEffect, useCallback } from "react";
import { Link } from "react-router-dom";
import { Heart, Trash2, MapPin, Maximize2, ArrowRight } from "lucide-react";
import { favoriteService } from "../services/favoriteService";
import type { RoomSummary } from "../types/room";

export default function FavoritesPage() {
  const [rooms, setRooms] = useState<RoomSummary[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [totalElements, setTotalElements] = useState<number>(0);
  const [currentPage, setCurrentPage] = useState<number>(0);
  const [totalPages, setTotalPages] = useState<number>(1);
  const [removingId, setRemovingId] = useState<string | null>(null);

  const fetchFavorites = useCallback(async (page = 0) => {
    setIsLoading(true);
    try {
      const res = await favoriteService.getMyFavoriteRooms(page, 12);
      setRooms(res.content || []);
      setTotalElements(res.totalElements || 0);
      setTotalPages(res.totalPages || 1);
      setCurrentPage(res.pageNumber || 0);
    } catch (error) {
      console.error("Lỗi khi tải danh sách yêu thích:", error);
      setRooms([]);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchFavorites(0);
  }, [fetchFavorites]);

  const handleRemoveFavorite = async (roomId: string, e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();

    setRemovingId(roomId);
    try {
      await favoriteService.removeFavorite(roomId);
      // Cập nhật state danh sách ngay lập tức
      setRooms((prev) => prev.filter((r) => r.id !== roomId));
      setTotalElements((prev) => Math.max(0, prev - 1));
    } catch (error) {
      console.error("Lỗi khi xóa khỏi yêu thích:", error);
    } finally {
      setRemovingId(null);
    }
  };

  return (
    <main className="favorites-page" style={{ padding: "40px 0 80px", minHeight: "80vh", background: "#f8fafc" }}>
      <div className="container" style={{ maxWidth: "1200px", margin: "0 auto", padding: "0 16px" }}>
        {/* Page Header */}
        <div
          style={{
            background: "linear-gradient(135deg, #1e3a8a 0%, #167c5a 100%)",
            borderRadius: "16px",
            padding: "36px 32px",
            color: "#ffffff",
            marginBottom: "32px",
            boxShadow: "0 10px 25px rgba(22, 124, 90, 0.15)",
            display: "flex",
            flexDirection: "column",
            gap: "12px",
          }}
        >
          <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
            <span
              style={{
                display: "inline-flex",
                alignItems: "center",
                justifyContent: "center",
                width: "40px",
                height: "40px",
                borderRadius: "10px",
                background: "rgba(255, 255, 255, 0.2)",
              }}
            >
              <Heart size={22} fill="#ef4444" color="#ef4444" />
            </span>
            <h1 style={{ fontSize: "28px", fontWeight: 800, margin: 0 }}>Phòng trọ yêu thích</h1>
          </div>
          <p style={{ margin: 0, opacity: 0.9, fontSize: "15px", maxWidth: "650px" }}>
            Danh sách các phòng trọ bạn đã lưu để tiện so sánh, theo dõi giá và liên hệ với chủ nhà khi sẵn sàng thuê.
          </p>
          <div
            style={{
              display: "inline-block",
              marginTop: "4px",
              background: "rgba(255, 255, 255, 0.18)",
              padding: "4px 14px",
              borderRadius: "20px",
              fontSize: "13px",
              fontWeight: 600,
              width: "fit-content",
            }}
          >
            Đang lưu {totalElements} phòng trọ
          </div>
        </div>

        {/* Content Area */}
        {isLoading ? (
          <div
            style={{
              textAlign: "center",
              padding: "80px 0",
              color: "#64748b",
            }}
          >
            <div
              style={{
                width: "40px",
                height: "40px",
                border: "4px solid #e2e8f0",
                borderTopColor: "#167c5a",
                borderRadius: "50%",
                animation: "spin 1s linear infinite",
                margin: "0 auto 16px",
              }}
            />
            <p>Đang tải danh sách phòng yêu thích...</p>
          </div>
        ) : rooms.length === 0 ? (
          <div
            style={{
              textAlign: "center",
              padding: "70px 24px",
              background: "#ffffff",
              borderRadius: "16px",
              border: "1px dashed #cbd5e1",
              maxWidth: "600px",
              margin: "40px auto",
            }}
          >
            <div
              style={{
                width: "72px",
                height: "72px",
                borderRadius: "50%",
                background: "#fef2f2",
                display: "grid",
                placeItems: "center",
                margin: "0 auto 20px",
              }}
            >
              <Heart size={36} color="#ef4444" />
            </div>
            <h3 style={{ fontSize: "20px", fontWeight: 700, color: "#1e293b", marginBottom: "8px" }}>
              Chưa có phòng trọ yêu thích
            </h3>
            <p style={{ color: "#64748b", fontSize: "14px", lineHeight: "1.6", marginBottom: "24px" }}>
              Bạn chưa lưu phòng trọ nào vào danh sách yêu thích. Hãy bấm vào biểu tượng trái tim trên các thẻ phòng để lưu lại và theo dõi thuận tiện hơn!
            </p>
            <Link
              to="/rooms"
              className="btn btn-primary"
              style={{
                display: "inline-flex",
                alignItems: "center",
                gap: "8px",
                padding: "12px 24px",
                fontSize: "15px",
                textDecoration: "none",
                borderRadius: "10px",
                fontWeight: 600,
              }}
            >
              Khám phá phòng trọ ngay <ArrowRight size={18} />
            </Link>
          </div>
        ) : (
          <>
            <div
              style={{
                display: "grid",
                gridTemplateColumns: "repeat(auto-fill, minmax(280px, 1fr))",
                gap: "24px",
              }}
            >
              {rooms.map((room) => {
                const imageUrl =
                  room.primaryImageUrl ||
                  "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=800&q=80";
                const locationText = `${room.district}, ${room.city || "TP. Hồ Chí Minh"}`;

                return (
                  <article
                    key={room.id}
                    className="room-card favorite-card"
                    style={{
                      background: "#ffffff",
                      border: "1px solid #e2e8f0",
                      borderRadius: "12px",
                      overflow: "hidden",
                      transition: "all 0.2s ease",
                      position: "relative",
                      display: "flex",
                      flexDirection: "column",
                    }}
                  >
                    <div className="room-image-wrapper" style={{ height: "200px", position: "relative" }}>
                      <img
                        src={imageUrl}
                        alt={room.title}
                        className="room-image"
                        loading="lazy"
                        style={{ width: "100%", height: "100%", objectFit: "cover" }}
                      />

                      {/* Remove Favorite Button */}
                      <button
                        type="button"
                        onClick={(e) => handleRemoveFavorite(room.id, e)}
                        disabled={removingId === room.id}
                        title="Bỏ lưu yêu thích"
                        style={{
                          position: "absolute",
                          top: "12px",
                          right: "12px",
                          width: "36px",
                          height: "36px",
                          borderRadius: "50%",
                          border: "none",
                          background: "rgba(255, 255, 255, 0.92)",
                          boxShadow: "0 2px 8px rgba(0,0,0,0.15)",
                          display: "grid",
                          placeItems: "center",
                          cursor: "pointer",
                          color: "#ef4444",
                          transition: "all 0.2s ease",
                        }}
                        onMouseEnter={(e) => {
                          e.currentTarget.style.transform = "scale(1.1)";
                          e.currentTarget.style.background = "#fee2e2";
                        }}
                        onMouseLeave={(e) => {
                          e.currentTarget.style.transform = "scale(1)";
                          e.currentTarget.style.background = "rgba(255, 255, 255, 0.92)";
                        }}
                      >
                        <Heart size={18} fill="#ef4444" color="#ef4444" />
                      </button>
                    </div>

                    <div className="room-card-body" style={{ padding: "16px", flex: 1, display: "flex", flexDirection: "column" }}>
                      <Link
                        to={`/rooms/${room.id}`}
                        style={{
                          textDecoration: "none",
                          color: "#1e293b",
                          fontWeight: 700,
                          fontSize: "16px",
                          lineHeight: "1.4",
                          marginBottom: "8px",
                          display: "-webkit-box",
                          WebkitLineClamp: 2,
                          WebkitBoxOrient: "vertical",
                          overflow: "hidden",
                          minHeight: "44px",
                        }}
                      >
                        {room.title}
                      </Link>

                      <p
                        style={{
                          fontSize: "18px",
                          fontWeight: 800,
                          color: "#167c5a",
                          margin: "0 0 10px 0",
                        }}
                      >
                        {room.price ? room.price.toLocaleString("vi-VN") : 0} đ/tháng
                      </p>

                      <div
                        style={{
                          display: "flex",
                          alignItems: "center",
                          justifyContent: "space-between",
                          fontSize: "13px",
                          color: "#64748b",
                          marginBottom: "16px",
                        }}
                      >
                        <span style={{ display: "flex", alignItems: "center", gap: "4px" }}>
                          <MapPin size={15} color="#94a3b8" />
                          {locationText}
                        </span>

                        <span style={{ display: "flex", alignItems: "center", gap: "4px" }}>
                          <Maximize2 size={15} color="#94a3b8" />
                          {room.area} m²
                        </span>
                      </div>

                      <div style={{ marginTop: "auto", display: "flex", gap: "8px" }}>
                        <Link
                          to={`/rooms/${room.id}`}
                          className="btn btn-primary"
                          style={{
                            flex: 1,
                            textAlign: "center",
                            textDecoration: "none",
                            padding: "8px 12px",
                            fontSize: "13px",
                            fontWeight: 600,
                            borderRadius: "8px",
                          }}
                        >
                          Xem chi tiết
                        </Link>
                        <button
                          type="button"
                          onClick={(e) => handleRemoveFavorite(room.id, e)}
                          title="Xóa khỏi yêu thích"
                          style={{
                            padding: "8px 12px",
                            background: "#fff1f2",
                            border: "1px solid #fecdd3",
                            borderRadius: "8px",
                            color: "#e11d48",
                            cursor: "pointer",
                            display: "grid",
                            placeItems: "center",
                          }}
                        >
                          <Trash2 size={16} />
                        </button>
                      </div>
                    </div>
                  </article>
                );
              })}
            </div>

            {/* Pagination */}
            {totalPages > 1 && (
              <div
                style={{
                  marginTop: "40px",
                  display: "flex",
                  justifyContent: "center",
                  alignItems: "center",
                  gap: "8px",
                }}
              >
                <button
                  type="button"
                  disabled={currentPage === 0}
                  onClick={() => fetchFavorites(currentPage - 1)}
                  style={{
                    padding: "8px 16px",
                    borderRadius: "8px",
                    border: "1px solid #cbd5e1",
                    background: currentPage === 0 ? "#f1f5f9" : "#ffffff",
                    cursor: currentPage === 0 ? "not-allowed" : "pointer",
                    fontSize: "14px",
                  }}
                >
                  Trang trước
                </button>
                <span style={{ fontSize: "14px", color: "#64748b", margin: "0 8px" }}>
                  Trang {currentPage + 1} / {totalPages}
                </span>
                <button
                  type="button"
                  disabled={currentPage >= totalPages - 1}
                  onClick={() => fetchFavorites(currentPage + 1)}
                  style={{
                    padding: "8px 16px",
                    borderRadius: "8px",
                    border: "1px solid #cbd5e1",
                    background: currentPage >= totalPages - 1 ? "#f1f5f9" : "#ffffff",
                    cursor: currentPage >= totalPages - 1 ? "not-allowed" : "pointer",
                    fontSize: "14px",
                  }}
                >
                  Trang sau
                </button>
              </div>
            )}
          </>
        )}
      </div>
    </main>
  );
}
