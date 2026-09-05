import { useState, useEffect } from "react";
import RoomCard from "../components/RoomCard";
import { roomService } from "../services/roomService";
import type { Amenity, RoomSummary } from "../types/room";

export default function RoomsPage() {
  const [rooms, setRooms] = useState<RoomSummary[]>([]);
  const [amenities, setAmenities] = useState<Amenity[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  // Filters
  const [keyword, setKeyword] = useState("");
  const [selectedDistrict, setSelectedDistrict] = useState("");
  const [priceRange, setPriceRange] = useState("");
  const [selectedAmenity, setSelectedAmenity] = useState("");

  const districts = [
    "Cầu Giấy",
    "Đống Đa",
    "Nam Từ Liêm",
    "Thanh Xuân",
    "Ba Đình",
    "Hai Bà Trưng",
    "Tây Hồ",
    "Hà Đông",
    "Hoàng Mai",
    "Bắc Từ Liêm",
    "Long Biên",
  ];

  // Tải danh mục tiện ích
  useEffect(() => {
    roomService.getAllAmenities()
      .then(setAmenities)
      .catch((err) => console.warn("Lỗi tải tiện ích:", err));
  }, []);

  // Tải danh sách phòng trọ từ API
  useEffect(() => {
    setIsLoading(true);

    let minPrice: number | undefined;
    let maxPrice: number | undefined;

    if (priceRange === "under3") {
      maxPrice = 3000000;
    } else if (priceRange === "3to5") {
      minPrice = 3000000;
      maxPrice = 5000000;
    } else if (priceRange === "above5") {
      minPrice = 5000000;
    }

    const amenityId = selectedAmenity ? parseInt(selectedAmenity, 10) : undefined;
    const districtFilter = selectedDistrict || (keyword.trim() ? keyword.trim() : undefined);

    roomService.getApprovedRooms({
      district: districtFilter,
      minPrice,
      maxPrice,
      amenityId,
      size: 20,
    })
      .then((res) => {
        setRooms(res.content || []);
      })
      .catch((err) => {
        console.error("Lỗi khi tải phòng trọ:", err);
        setRooms([]);
      })
      .finally(() => {
        setIsLoading(false);
      });
  }, [selectedDistrict, priceRange, selectedAmenity, keyword]);

  return (
    <main className="page">
      <div className="container">
        <div className="rooms-header">
          <div>
            <span className="section-label">Khám phá</span>
            <h1>Danh sách phòng trọ</h1>
            <p>Hàng ngàn phòng trọ chất lượng, chính chủ được kiểm duyệt tại Hà Nội.</p>
          </div>
        </div>

        {/* Bộ lọc tìm kiếm */}
        <div className="rooms-search" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '12px' }}>
          <input
            type="text"
            placeholder="Tìm theo từ khóa, tên đường..."
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
          />

          <select
            value={selectedDistrict}
            onChange={(e) => setSelectedDistrict(e.target.value)}
          >
            <option value="">Tất cả quận/huyện</option>
            {districts.map((d) => (
              <option key={d} value={d}>{d}</option>
            ))}
          </select>

          <select
            value={priceRange}
            onChange={(e) => setPriceRange(e.target.value)}
          >
            <option value="">Tất cả mức giá</option>
            <option value="under3">Dưới 3 triệu</option>
            <option value="3to5">3 - 5 triệu</option>
            <option value="above5">Trên 5 triệu</option>
          </select>

          <select
            value={selectedAmenity}
            onChange={(e) => setSelectedAmenity(e.target.value)}
          >
            <option value="">Tất cả tiện ích</option>
            {amenities.map((a) => (
              <option key={a.id} value={a.id}>{a.name}</option>
            ))}
          </select>
        </div>

        <div className="result-header">
          <strong>
            {isLoading ? "Đang tìm kiếm..." : `${rooms.length} phòng được tìm thấy`}
          </strong>
        </div>

        {isLoading ? (
          <div style={{ textAlign: 'center', padding: '60px 0' }}>
            <p>Đang tải dữ liệu phòng trọ từ máy chủ...</p>
          </div>
        ) : (
          <>
            <div className="room-grid">
              {rooms.map((room) => (
                <RoomCard room={room} key={room.id} />
              ))}
            </div>

            {rooms.length === 0 && (
              <div className="empty-state" style={{ textAlign: 'center', padding: '60px 0' }}>
                <h3>Không tìm thấy phòng phù hợp</h3>
                <p>Hãy thử điều chỉnh bộ lọc hoặc từ khóa tìm kiếm khác.</p>
              </div>
            )}
          </>
        )}
      </div>
    </main>
  );
}