import { useState, useEffect, useCallback } from "react";
import { useSearchParams } from "react-router-dom";
import RoomCard from "../components/RoomCard";
import { roomService } from "../services/roomService";
import { searchHistoryService } from "../services/searchHistoryService";
import { favoriteService } from "../services/favoriteService";
import { useAuth } from "../hooks/useAuth";
import type { SearchHistoryItem } from "../types/favorite";
import type {
  Amenity,
  DistrictCountResponse,
  ParsedQueryResponse,
  RoomSearchRequest,
  RoomSummary,
} from "../types/room";

const PROMPT_SUGGESTIONS = [
  "Bình Thạnh dưới 4 triệu có điều hòa và máy giặt",
  "Studio Quận 1 có ban công",
  "Phòng trọ khép kín Thủ Đức dưới 5tr",
  "Gò Vấp tầm 3 triệu có tủ lạnh",
];

const PRICE_PRESETS = [
  { label: "Tất cả", min: undefined, max: undefined },
  { label: "Dưới 3 triệu", min: undefined, max: 3000000 },
  { label: "3 - 5 triệu", min: 3000000, max: 5000000 },
  { label: "5 - 8 triệu", min: 5000000, max: 8000000 },
  { label: "Trên 8 triệu", min: 8000000, max: undefined },
];

const AREA_PRESETS = [
  { label: "Tất cả", min: undefined, max: undefined },
  { label: "Dưới 20m²", min: undefined, max: 20 },
  { label: "20 - 30m²", min: 20, max: 30 },
  { label: "30 - 45m²", min: 30, max: 45 },
  { label: "Trên 45m²", min: 45, max: undefined },
];

export default function RoomsPage() {
  const [searchParams] = useSearchParams();
  const urlDistrict = searchParams.get("district") || "";
  const urlKeyword = searchParams.get("q") || searchParams.get("keyword") || "";

  const [rooms, setRooms] = useState<RoomSummary[]>([]);
  const [amenities, setAmenities] = useState<Amenity[]>([]);
  const [districtCounts, setDistrictCounts] = useState<DistrictCountResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  // AI NLP Search state
  const [nlpQuery, setNlpQuery] = useState("");
  const [isNlpLoading, setIsNlpLoading] = useState(false);
  const [parsedQuery, setParsedQuery] = useState<ParsedQueryResponse | null>(null);

  // Advanced Filters State
  const [showAdvancedFilters, setShowAdvancedFilters] = useState(Boolean(urlDistrict || urlKeyword));
  const [keyword, setKeyword] = useState(urlKeyword);
  const [selectedDistrict, setSelectedDistrict] = useState(urlDistrict);
  const [minPrice, setMinPrice] = useState<number | undefined>(undefined);
  const [maxPrice, setMaxPrice] = useState<number | undefined>(undefined);
  const [minArea, setMinArea] = useState<number | undefined>(undefined);
  const [maxArea, setMaxArea] = useState<number | undefined>(undefined);
  const [selectedAmenityIds, setSelectedAmenityIds] = useState<number[]>([]);
  const [sortBy, setSortBy] = useState<"createdAt" | "price" | "area" | "viewCount">("createdAt");
  const [sortDirection, setSortDirection] = useState<"asc" | "desc">("desc");

  // Search History State
  const { isAuthenticated } = useAuth();
  const [recentSearches, setRecentSearches] = useState<SearchHistoryItem[]>([]);

  const loadRecentSearches = useCallback(async () => {
    if (isAuthenticated) {
      try {
        const items = await searchHistoryService.getRecentSearches();
        setRecentSearches(items);
      } catch (err) {
        console.warn("Không thể tải lịch sử tìm kiếm:", err);
      }
    } else {
      setRecentSearches([]);
    }
  }, [isAuthenticated]);

  useEffect(() => {
    loadRecentSearches();
  }, [loadRecentSearches]);

  // Favorite Rooms State: theo dõi các phòng người dùng đã lưu để hiển thị tim đỏ
  const [favoriteRoomIds, setFavoriteRoomIds] = useState<Set<string>>(new Set());

  useEffect(() => {
    if (isAuthenticated) {
      favoriteService.getMyFavoriteRoomIds()
        .then((ids) => setFavoriteRoomIds(new Set(ids)))
        .catch((err) => console.warn("Lỗi tải danh sách ID phòng yêu thích:", err));
    } else {
      setFavoriteRoomIds(new Set());
    }
  }, [isAuthenticated]);

  const handleDeleteHistoryItem = async (id: string, e: React.MouseEvent) => {
    e.stopPropagation();
    try {
      await searchHistoryService.deleteSearchHistoryItem(id);
      setRecentSearches((prev) => prev.filter((item) => item.id !== id));
    } catch (err) {
      console.error("Lỗi khi xóa mục lịch sử:", err);
    }
  };

  const handleClearAllHistory = async () => {
    try {
      await searchHistoryService.clearAllSearchHistory();
      setRecentSearches([]);
    } catch (err) {
      console.error("Lỗi khi xóa toàn bộ lịch sử:", err);
    }
  };

  const handleApplyHistory = (item: SearchHistoryItem) => {
    if (item.queryText) {
      setNlpQuery(item.queryText);
      handleNlpSearch(item.queryText);
    } else {
      if (item.district) setSelectedDistrict(item.district);
      if (item.minPrice !== undefined) setMinPrice(item.minPrice);
      if (item.maxPrice !== undefined) setMaxPrice(item.maxPrice);
      setShowAdvancedFilters(true);
    }
  };

  // Load amenities and district summaries on mount
  useEffect(() => {
    roomService.getAllAmenities()
      .then(setAmenities)
      .catch((err) => console.warn("Lỗi tải tiện ích:", err));

    roomService.getDistrictRoomCounts()
      .then(setDistrictCounts)
      .catch((err) => console.warn("Lỗi tải danh sách quận:", err));
  }, []);

  // Fetch rooms using multi-criteria search
  const executeSearch = useCallback(() => {
    setIsLoading(true);

    const searchParams: RoomSearchRequest = {
      keyword: keyword.trim() || undefined,
      district: selectedDistrict || undefined,
      minPrice,
      maxPrice,
      minArea,
      maxArea,
      amenityIds: selectedAmenityIds.length > 0 ? selectedAmenityIds : undefined,
      sortBy,
      sortDirection,
      page: 0,
      size: 24,
    };

    roomService.searchRooms(searchParams)
      .then((res) => {
        setRooms(res.content || []);
        if (isAuthenticated && (keyword.trim() || selectedDistrict || minPrice || maxPrice)) {
          searchHistoryService.saveSearchHistory({
            queryText: keyword.trim() || undefined,
            district: selectedDistrict || undefined,
            minPrice,
            maxPrice,
          }).then(() => loadRecentSearches()).catch(() => {});
        }
      })
      .catch((err) => {
        console.error("Lỗi khi tìm kiếm phòng trọ:", err);
        setRooms([]);
      })
      .finally(() => {
        setIsLoading(false);
      });
  }, [keyword, selectedDistrict, minPrice, maxPrice, minArea, maxArea, selectedAmenityIds, sortBy, sortDirection, isAuthenticated, loadRecentSearches]);

  // Trigger search whenever manual filters change (if not in pure NLP mode)
  useEffect(() => {
    if (!parsedQuery) {
      executeSearch();
    }
  }, [executeSearch, parsedQuery]);

  // Execute AI NLP Search
  const handleNlpSearch = async (queryTextToRun?: string) => {
    const query = (queryTextToRun || nlpQuery).trim();
    if (!query) return;

    setIsNlpLoading(true);
    setIsLoading(true);

    try {
      const res = await roomService.searchByNlp(query, 0, 24);
      setParsedQuery(res.parsedQuery);
      setRooms(res.results.content || []);

      // If user passed a suggestion prompt, update input field
      if (queryTextToRun) {
        setNlpQuery(queryTextToRun);
      }

      if (isAuthenticated) {
        searchHistoryService.saveSearchHistory({ queryText: query })
          .then(() => loadRecentSearches())
          .catch(() => {});
      }
    } catch (err) {
      console.error("Lỗi khi tìm kiếm bằng AI:", err);
      // Fallback: run normal search
      executeSearch();
    } finally {
      setIsNlpLoading(false);
      setIsLoading(false);
    }
  };

  // Clear AI search and return to standard filter
  const handleClearAiSearch = () => {
    setParsedQuery(null);
    setNlpQuery("");
    executeSearch();
  };

  // Toggle single amenity ID
  const handleAmenityToggle = (id: number) => {
    setSelectedAmenityIds((prev) =>
      prev.includes(id) ? prev.filter((item) => item !== id) : [...prev, id]
    );
  };

  // Reset all manual filters
  const handleResetFilters = () => {
    setKeyword("");
    setSelectedDistrict("");
    setMinPrice(undefined);
    setMaxPrice(undefined);
    setMinArea(undefined);
    setMaxArea(undefined);
    setSelectedAmenityIds([]);
    setSortBy("createdAt");
    setSortDirection("desc");
    setParsedQuery(null);
    setNlpQuery("");
  };

  // Count active manual filters
  const activeFilterCount =
    (keyword ? 1 : 0) +
    (selectedDistrict ? 1 : 0) +
    (minPrice || maxPrice ? 1 : 0) +
    (minArea || maxArea ? 1 : 0) +
    selectedAmenityIds.length;

  return (
    <main className="page">
      <div className="container">
        {/* Header Title */}
        <div className="rooms-header" style={{ marginBottom: 20 }}>
          <div>
            <span className="section-label">Hệ thống tìm kiếm thông minh</span>
            <h1 style={{ margin: "8px 0" }}>Tìm kiếm & Lọc phòng trọ</h1>
            <p style={{ color: "#64748b", margin: 0 }}>
              Khám phá phòng trọ với trợ lý AI NLP bóc tách yêu cầu tự nhiên hoặc lọc theo đa tiêu chí.
            </p>
          </div>
        </div>

        {/* 1. AI SMART SEARCH BAR */}
        <div className="ai-search-card">
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
            <span className="ai-badge">
              ✨ Trợ lý AI NLP Tiếng Việt
            </span>
            <span style={{ fontSize: 12, color: "#059669", fontWeight: 600 }}>
              Hỗ trợ ngôn ngữ tự nhiên
            </span>
          </div>

          <form
            onSubmit={(e) => {
              e.preventDefault();
              handleNlpSearch();
            }}
            className="ai-input-group"
          >
            <input
              type="text"
              placeholder="Nhập câu tự nhiên: vd 'tìm phòng trọ khép kín ở Bình Thạnh dưới 4 triệu có điều hòa'..."
              value={nlpQuery}
              onChange={(e) => setNlpQuery(e.target.value)}
            />
            <button
              type="submit"
              className="btn btn-primary"
              disabled={isNlpLoading || !nlpQuery.trim()}
              style={{ minWidth: 140 }}
            >
              {isNlpLoading ? "Đang phân tích..." : "⚡ Tìm kiếm AI"}
            </button>
          </form>

          {/* Prompt chips suggestions */}
          <div className="prompt-chips">
            <span className="prompt-chips-label">Gợi ý tìm kiếm:</span>
            {PROMPT_SUGGESTIONS.map((prompt) => (
              <button
                key={prompt}
                type="button"
                className="prompt-chip"
                onClick={() => handleNlpSearch(prompt)}
              >
                💡 {prompt}
              </button>
            ))}
          </div>

          {/* Recent Searches Bar */}
          {isAuthenticated && recentSearches.length > 0 && (
            <div
              className="recent-searches-bar"
              style={{
                marginTop: "14px",
                paddingTop: "12px",
                borderTop: "1px dashed #e2e8f0",
                display: "flex",
                flexWrap: "wrap",
                alignItems: "center",
                gap: "8px",
              }}
            >
              <span
                style={{
                  fontSize: "13px",
                  color: "#475569",
                  fontWeight: 600,
                  display: "flex",
                  alignItems: "center",
                  gap: "4px",
                }}
              >
                🕒 Gần đây:
              </span>

              {recentSearches.map((item) => {
                const displayText =
                  item.queryText ||
                  `${item.district || ""} ${
                    item.maxPrice ? `dưới ${(item.maxPrice / 1000000).toFixed(0)}tr` : ""
                  }`.trim();

                if (!displayText) return null;

                return (
                  <span
                    key={item.id}
                    onClick={() => handleApplyHistory(item)}
                    style={{
                      display: "inline-flex",
                      alignItems: "center",
                      gap: "6px",
                      padding: "4px 10px",
                      background: "#f1f5f9",
                      borderRadius: "16px",
                      fontSize: "12px",
                      color: "#334155",
                      cursor: "pointer",
                      border: "1px solid #e2e8f0",
                      transition: "all 0.15s ease",
                    }}
                    onMouseEnter={(e) => {
                      e.currentTarget.style.background = "#e2e8f0";
                    }}
                    onMouseLeave={(e) => {
                      e.currentTarget.style.background = "#f1f5f9";
                    }}
                    title="Bấm để tìm kiếm lại theo nội dung này"
                  >
                    <span>{displayText}</span>
                    <button
                      type="button"
                      onClick={(e) => handleDeleteHistoryItem(item.id, e)}
                      style={{
                        background: "none",
                        border: "none",
                        color: "#94a3b8",
                        cursor: "pointer",
                        padding: "0 2px",
                        fontSize: "12px",
                        lineHeight: 1,
                        display: "inline-flex",
                        alignItems: "center",
                      }}
                      title="Xóa khỏi lịch sử"
                    >
                      ✕
                    </button>
                  </span>
                );
              })}

              <button
                type="button"
                onClick={handleClearAllHistory}
                style={{
                  background: "none",
                  border: "none",
                  color: "#94a3b8",
                  fontSize: "12px",
                  cursor: "pointer",
                  textDecoration: "underline",
                  marginLeft: "auto",
                  padding: "4px 6px",
                }}
                title="Xóa toàn bộ lịch sử tìm kiếm"
              >
                Xóa tất cả
              </button>
            </div>
          )}
        </div>

        {/* 2. AI INSIGHTS BANNER (IF AI SEARCH WAS TRIGGERED) */}
        {parsedQuery && (
          <div className="ai-insights-banner">
            <div className="ai-insights-header">
              <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                <span style={{ fontSize: 16 }}>🤖</span>
                <span>
                  AI đã trích xuất các tiêu chí từ câu: <em>"{parsedQuery.originalQuery}"</em>
                </span>
              </div>
              <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                <span
                  style={{
                    fontSize: 11,
                    padding: "2px 8px",
                    borderRadius: 4,
                    background: parsedQuery.source === "AI_SERVICE" ? "#dbeafe" : "#fef3c7",
                    color: parsedQuery.source === "AI_SERVICE" ? "#1e40af" : "#92400e",
                    fontWeight: 600,
                  }}
                >
                  {parsedQuery.source === "AI_SERVICE" ? "FastAPI AI Model" : "Vietnamese Regex Engine"}
                </span>
                <button
                  type="button"
                  onClick={handleClearAiSearch}
                  className="clear-filters-btn"
                  title="Xóa tìm kiếm AI và quay lại bộ lọc thông thường"
                >
                  ✕ Đóng phân tích
                </button>
              </div>
            </div>

            <div className="ai-entities-list">
              {parsedQuery.district && (
                <span className="ai-entity-tag highlight">
                  📍 Quận: <strong>{parsedQuery.district}</strong>
                </span>
              )}
              {parsedQuery.maxPrice && (
                <span className="ai-entity-tag highlight">
                  💰 Giá tối đa: <strong>{(parsedQuery.maxPrice / 1000000).toFixed(1)} triệu</strong>
                </span>
              )}
              {parsedQuery.minPrice && (
                <span className="ai-entity-tag highlight">
                  💰 Giá tối thiểu: <strong>{(parsedQuery.minPrice / 1000000).toFixed(1)} triệu</strong>
                </span>
              )}
              {parsedQuery.minArea && (
                <span className="ai-entity-tag highlight">
                  📐 Diện tích: <strong>&gt; {parsedQuery.minArea} m²</strong>
                </span>
              )}
              {parsedQuery.detectedAmenities && parsedQuery.detectedAmenities.length > 0 && (
                <span className="ai-entity-tag highlight">
                  🛋️ Tiện ích: <strong>{parsedQuery.detectedAmenities.join(", ")}</strong>
                </span>
              )}
              {parsedQuery.keyword && (
                <span className="ai-entity-tag highlight">
                  🏷️ Từ khóa: <strong>{parsedQuery.keyword}</strong>
                </span>
              )}
            </div>
          </div>
        )}

        {/* 3. ADVANCED FILTER TOGGLE & PANEL */}
        <div className="filter-panel">
          <div
            className="filter-panel-header"
            onClick={() => setShowAdvancedFilters(!showAdvancedFilters)}
          >
            <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
              <span style={{ fontWeight: 700, fontSize: 15, color: "#1e293b" }}>
                🔍 Bộ lọc nâng cao đa tiêu chí
              </span>
              {activeFilterCount > 0 && (
                <span
                  style={{
                    background: "#167c5a",
                    color: "white",
                    borderRadius: 9999,
                    fontSize: 11,
                    padding: "2px 8px",
                    fontWeight: 700,
                  }}
                >
                  {activeFilterCount}
                </span>
              )}
            </div>
            <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
              {activeFilterCount > 0 && (
                <button
                  type="button"
                  onClick={(e) => {
                    e.stopPropagation();
                    handleResetFilters();
                  }}
                  className="clear-filters-btn"
                >
                  Xóa tất cả
                </button>
              )}
              <span style={{ color: "#64748b", fontSize: 14 }}>
                {showAdvancedFilters ? "▲ Thu gọn" : "▼ Mở rộng bộ lọc"}
              </span>
            </div>
          </div>

          {/* Collapsible Filter Body */}
          {showAdvancedFilters && (
            <div style={{ marginTop: 16, paddingTop: 16, borderTop: "1px solid #e2e8f0" }}>
              <div className="filter-grid">
                {/* Từ khóa */}
                <div className="filter-group">
                  <label>Từ khóa</label>
                  <input
                    type="text"
                    placeholder="Tên đường, địa chỉ, gác lửng..."
                    value={keyword}
                    onChange={(e) => setKeyword(e.target.value)}
                  />
                </div>

                {/* Quận / Huyện */}
                <div className="filter-group">
                  <label>Quận / Huyện ({districtCounts.length})</label>
                  <select
                    value={selectedDistrict}
                    onChange={(e) => setSelectedDistrict(e.target.value)}
                  >
                    <option value="">Tất cả quận / huyện</option>
                    {districtCounts.length > 0
                      ? districtCounts.map((d) => (
                          <option key={d.district} value={d.district}>
                            {d.district} ({d.count} phòng)
                          </option>
                        ))
                      : [
                          "Quận 1",
                          "Quận 3",
                          "Quận 4",
                          "Quận 5",
                          "Quận 7",
                          "Quận 10",
                          "Bình Thạnh",
                          "Gò Vấp",
                          "Phú Nhuận",
                          "Tân Bình",
                          "Tân Phú",
                          "Thủ Đức",
                          "Bình Tân",
                          "Quận 8",
                          "Quận 12",
                        ].map((d) => (
                          <option key={d} value={d}>
                            {d}
                          </option>
                        ))}
                  </select>
                </div>

                {/* Khoảng giá */}
                <div className="filter-group">
                  <label>Mức giá thuê</label>
                  <select
                    value={
                      PRICE_PRESETS.findIndex(
                        (p) => p.min === minPrice && p.max === maxPrice
                      ) !== -1
                        ? PRICE_PRESETS.findIndex(
                            (p) => p.min === minPrice && p.max === maxPrice
                          )
                        : ""
                    }
                    onChange={(e) => {
                      const idx = parseInt(e.target.value, 10);
                      if (!isNaN(idx) && PRICE_PRESETS[idx]) {
                        setMinPrice(PRICE_PRESETS[idx].min);
                        setMaxPrice(PRICE_PRESETS[idx].max);
                      } else {
                        setMinPrice(undefined);
                        setMaxPrice(undefined);
                      }
                    }}
                  >
                    {PRICE_PRESETS.map((p, idx) => (
                      <option key={p.label} value={idx}>
                        {p.label}
                      </option>
                    ))}
                  </select>
                </div>

                {/* Khoảng diện tích */}
                <div className="filter-group">
                  <label>Diện tích (m²)</label>
                  <select
                    value={
                      AREA_PRESETS.findIndex(
                        (a) => a.min === minArea && a.max === maxArea
                      ) !== -1
                        ? AREA_PRESETS.findIndex(
                            (a) => a.min === minArea && a.max === maxArea
                          )
                        : ""
                    }
                    onChange={(e) => {
                      const idx = parseInt(e.target.value, 10);
                      if (!isNaN(idx) && AREA_PRESETS[idx]) {
                        setMinArea(AREA_PRESETS[idx].min);
                        setMaxArea(AREA_PRESETS[idx].max);
                      } else {
                        setMinArea(undefined);
                        setMaxArea(undefined);
                      }
                    }}
                  >
                    {AREA_PRESETS.map((a, idx) => (
                      <option key={a.label} value={idx}>
                        {a.label}
                      </option>
                    ))}
                  </select>
                </div>

                {/* Sắp xếp */}
                <div className="filter-group">
                  <label>Sắp xếp theo</label>
                  <div style={{ display: "flex", gap: 8 }}>
                    <select
                      value={sortBy}
                      onChange={(e) => setSortBy(e.target.value as any)}
                      style={{ flex: 1 }}
                    >
                      <option value="createdAt">Ngày đăng mới nhất</option>
                      <option value="price">Giá thuê</option>
                      <option value="area">Diện tích</option>
                      <option value="viewCount">Lượt xem nhiều</option>
                    </select>
                    <button
                      type="button"
                      className="btn btn-outline"
                      onClick={() => setSortDirection(sortDirection === "asc" ? "desc" : "asc")}
                      title={sortDirection === "asc" ? "Tăng dần" : "Giảm dần"}
                      style={{ minHeight: 40, padding: "0 12px" }}
                    >
                      {sortDirection === "asc" ? "↑ Tăng" : "↓ Giảm"}
                    </button>
                  </div>
                </div>
              </div>

              {/* Lọc Tiện ích (AND logic) */}
              <div style={{ marginTop: 20 }}>
                <label style={{ fontSize: 12, fontWeight: 700, color: "#475569", textTransform: "uppercase" }}>
                  Tiện ích bắt buộc (Chọn nhiều - AND logic):
                </label>
                <div className="amenities-checkbox-grid">
                  {amenities.map((amenity) => {
                    const isChecked = selectedAmenityIds.includes(amenity.id);
                    return (
                      <div
                        key={amenity.id}
                        className={`amenity-checkbox-item ${isChecked ? "checked" : ""}`}
                        onClick={() => handleAmenityToggle(amenity.id)}
                      >
                        <input
                          type="checkbox"
                          checked={isChecked}
                          onChange={() => {}}
                          style={{ cursor: "pointer" }}
                        />
                        <span>{amenity.icon || "✓"}</span>
                        <span>{amenity.name}</span>
                      </div>
                    );
                  })}
                </div>
              </div>
            </div>
          )}
        </div>

        {/* 4. ACTIVE FILTERS PILLS (IF ANY) */}
        {activeFilterCount > 0 && !parsedQuery && (
          <div className="active-filters-bar">
            <span style={{ fontSize: 12, fontWeight: 600, color: "#64748b" }}>
              Đang lọc theo:
            </span>
            {keyword && (
              <span className="active-filter-tag">
                Từ khóa: "{keyword}"
                <button type="button" onClick={() => setKeyword("")}>✕</button>
              </span>
            )}
            {selectedDistrict && (
              <span className="active-filter-tag">
                Quận: {selectedDistrict}
                <button type="button" onClick={() => setSelectedDistrict("")}>✕</button>
              </span>
            )}
            {(minPrice || maxPrice) && (
              <span className="active-filter-tag">
                Giá: {minPrice ? `${minPrice / 1000000}tr` : "0"} - {maxPrice ? `${maxPrice / 1000000}tr` : "∞"}
                <button
                  type="button"
                  onClick={() => {
                    setMinPrice(undefined);
                    setMaxPrice(undefined);
                  }}
                >
                  ✕
                </button>
              </span>
            )}
            {(minArea || maxArea) && (
              <span className="active-filter-tag">
                Diện tích: {minArea ? `${minArea}m²` : "0"} - {maxArea ? `${maxArea}m²` : "∞"}
                <button
                  type="button"
                  onClick={() => {
                    setMinArea(undefined);
                    setMaxArea(undefined);
                  }}
                >
                  ✕
                </button>
              </span>
            )}
            {selectedAmenityIds.map((id) => {
              const a = amenities.find((item) => item.id === id);
              return (
                <span key={id} className="active-filter-tag">
                  {a?.name || `Tiện ích #${id}`}
                  <button type="button" onClick={() => handleAmenityToggle(id)}>✕</button>
                </span>
              );
            })}
            <button
              type="button"
              className="clear-filters-btn"
              onClick={handleResetFilters}
            >
              Xóa tất cả
            </button>
          </div>
        )}

        {/* 5. RESULTS HEADER */}
        <div className="result-header" style={{ marginBottom: 16 }}>
          <strong>
            {isLoading
              ? "Đang tìm kiếm phòng trọ..."
              : `${rooms.length} phòng trọ phù hợp được tìm thấy`}
          </strong>
        </div>

        {/* 6. ROOM CARDS GRID & EMPTY STATE */}
        {isLoading ? (
          <div style={{ textAlign: "center", padding: "80px 0" }}>
            <div style={{ fontSize: 32, marginBottom: 12 }}>⚡</div>
            <p style={{ color: "#64748b", fontWeight: 500 }}>Đang tải và lọc dữ liệu từ máy chủ...</p>
          </div>
        ) : (
          <>
            <div className="room-grid">
              {rooms.map((room) => (
                <RoomCard
                  room={room}
                  key={room.id}
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

            {rooms.length === 0 && (
              <div className="empty-state" style={{ textAlign: "center", padding: "70px 20px" }}>
                <div style={{ fontSize: 48, marginBottom: 16 }}>🏡</div>
                <h3 style={{ fontSize: 20, marginBottom: 8 }}>Không tìm thấy phòng trọ phù hợp</h3>
                <p style={{ color: "#64748b", maxWidth: 450, margin: "0 auto 20px" }}>
                  Hãy thử nới lỏng các tiêu chí lọc giá, diện tích, hoặc thử tìm kiếm bằng câu tự nhiên khác.
                </p>
                <button
                  type="button"
                  className="btn btn-primary"
                  onClick={handleResetFilters}
                >
                  Đặt lại bộ lọc
                </button>
              </div>
            )}
          </>
        )}
      </div>
    </main>
  );
}