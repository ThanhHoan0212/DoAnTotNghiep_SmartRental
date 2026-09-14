-- ==============================================================================
-- Flyway Migration V6: Tạo bảng Quản lý Yêu thích (favorites) và Lịch sử tìm kiếm (search_histories)
-- ==============================================================================

-- 1. Bảng lưu trữ phòng trọ yêu thích (Favorites)
CREATE TABLE favorites (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    room_id UUID NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_room UNIQUE (user_id, room_id)
);

CREATE INDEX idx_favorites_user_id ON favorites(user_id);
CREATE INDEX idx_favorites_room_id ON favorites(room_id);

-- 2. Bảng lưu trữ lịch sử tìm kiếm (Search Histories)
CREATE TABLE search_histories (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    query_text VARCHAR(255),
    district VARCHAR(100),
    min_price DOUBLE PRECISION,
    max_price DOUBLE PRECISION,
    searched_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_search_histories_user_id ON search_histories(user_id);
CREATE INDEX idx_search_histories_searched_at ON search_histories(searched_at DESC);
