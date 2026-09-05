-- ==============================================================================
-- Flyway Migration V3: Tao bang amenities, rooms, room_images, room_amenities
-- ==============================================================================

-- 1. Bang danh muc tien ich phong tro (amenities)
CREATE TABLE IF NOT EXISTS amenities (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    icon VARCHAR(50)
);

-- 2. Bang tin dang phong tro (rooms)
CREATE TABLE IF NOT EXISTS rooms (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    landlord_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    price NUMERIC(12, 2) NOT NULL,
    area NUMERIC(6, 2) NOT NULL,
    address VARCHAR(255) NOT NULL,
    ward VARCHAR(100),
    district VARCHAR(100) NOT NULL,
    city VARCHAR(100) NOT NULL DEFAULT 'Hà Nội',
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    view_count BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_rooms_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'HIDDEN'))
);

CREATE INDEX IF NOT EXISTS idx_rooms_landlord_id ON rooms(landlord_id);
CREATE INDEX IF NOT EXISTS idx_rooms_status ON rooms(status);
CREATE INDEX IF NOT EXISTS idx_rooms_district ON rooms(district);
CREATE INDEX IF NOT EXISTS idx_rooms_price ON rooms(price);
CREATE INDEX IF NOT EXISTS idx_rooms_area ON rooms(area);

-- 3. Bang hinh anh phong tro (room_images)
CREATE TABLE IF NOT EXISTS room_images (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id UUID NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    image_url VARCHAR(500) NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_room_images_room_id ON room_images(room_id);

-- 4. Bang trung gian quan he Many-to-Many giua rooms va amenities (room_amenities)
CREATE TABLE IF NOT EXISTS room_amenities (
    room_id UUID NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    amenity_id BIGINT NOT NULL REFERENCES amenities(id) ON DELETE CASCADE,
    PRIMARY KEY (room_id, amenity_id)
);

CREATE INDEX IF NOT EXISTS idx_room_amenities_amenity_id ON room_amenities(amenity_id);

-- 5. Seed du lieu khoi tao: 10 tien ich phong tro co ban
INSERT INTO amenities (id, name, icon) VALUES
    (1, 'Wifi tốc độ cao', 'wifi'),
    (2, 'Điều hòa nhiệt độ', 'wind'),
    (3, 'Bình nóng lạnh', 'flame'),
    (4, 'Chỗ để xe miễn phí', 'car'),
    (5, 'Tủ lạnh', 'box'),
    (6, 'Máy giặt chung', 'refresh-cw'),
    (7, 'Giờ giấc tự do', 'clock'),
    (8, 'Ban công thoáng mát', 'sun'),
    (9, 'Khu vực bếp riêng', 'utensils'),
    (10, 'Vệ sinh khép kín', 'shield-check')
ON CONFLICT (id) DO NOTHING;

SELECT setval('amenities_id_seq', (SELECT MAX(id) FROM amenities));

-- 6. Seed tin dang mau cho tai khoan Admin de he thong co san du lieu demo
INSERT INTO rooms (id, landlord_id, title, description, price, area, address, ward, district, city, latitude, longitude, status, view_count, created_at, updated_at)
VALUES
    (
        'b0000000-0000-0000-0000-000000000001',
        'a0000000-0000-0000-0000-000000000001',
        'Phòng trọ cao cấp full nội thất gần ĐH Quốc Gia Cầu Giấy',
        'Phòng mới xây 100%, đầy đủ điều hòa, nóng lạnh, giường tủ gỗ sang trọng. Giờ giấc tự do, không chung chủ, an ninh camera 24/7. Vị trí trung tâm thuận tiện đi lại ra Xuân Thủy, Cầu Giấy, gần bến xe buýt và ga tàu điện ngầm.',
        3800000,
        28,
        'Số 18 Ngõ 175 Cầu Giấy',
        'Dịch Vọng',
        'Cầu Giấy',
        'Hà Nội',
        21.0333,
        105.7944,
        'APPROVED',
        142,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'b0000000-0000-0000-0000-000000000002',
        'a0000000-0000-0000-0000-000000000001',
        'Căn hộ mini ban công rộng rãi tại Đống Đa',
        'Căn hộ studio có ban công view đẹp, ánh sáng tự nhiên ngập tràn. Trang bị bếp từ hút mùi riêng biệt, máy giặt riêng trong phòng. Khu dân trí cao, yên tĩnh, thích hợp cho người đi làm hoặc sinh viên các trường Bách Khoa, Kinh Tế.',
        4500000,
        35,
        'Ngõ 82 Phạm Ngọc Thạch',
        'Trung Tự',
        'Đống Đa',
        'Hà Nội',
        21.0094,
        105.8342,
        'APPROVED',
        98,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'b0000000-0000-0000-0000-000000000003',
        'a0000000-0000-0000-0000-000000000001',
        'Phòng khép kín giá rẻ cho sinh viên Nam Từ Liêm',
        'Phòng rộng rãi sạch sẽ, vệ sinh khép kín, có gác lửng để đồ hoặc ngủ. Gần chợ dân sinh, siêu thị WinMart, cách bến xe Mỹ Đình 1km. Giá điện nước tính theo đồng hồ công tơ riêng, có chỗ để xe máy tầng 1 khóa vân tay an toàn.',
        2500000,
        22,
        'Số 45 Đình Thôn',
        'Mỹ Đình 1',
        'Nam Từ Liêm',
        'Hà Nội',
        21.0189,
        105.7761,
        'APPROVED',
        230,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'b0000000-0000-0000-0000-000000000004',
        'a0000000-0000-0000-0000-000000000001',
        'Phòng trọ mới tinh ban công ngập nắng gần ĐH Hà Nội',
        'Phòng trọ khép kín mới 100%, có thang máy di chuyển, bảo vệ trực ngày đêm. Đầy đủ điều hòa Inverter tiết kiệm điện, bình nóng lạnh Ariston, kệ bếp nấu ăn xinh xắn. Vị trí gần các trường ĐH Khoa học Tự nhiên, ĐH Hà Nội.',
        3200000,
        25,
        'Ngõ 336 Nguyễn Trãi',
        'Thanh Xuân Trung',
        'Thanh Xuân',
        'Hà Nội',
        20.9942,
        105.8083,
        'APPROVED',
        75,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    )
ON CONFLICT (id) DO NOTHING;

-- 7. Seed anh phong tro mau
INSERT INTO room_images (id, room_id, image_url, is_primary) VALUES
    ('c0000000-0000-0000-0000-000000000001', 'b0000000-0000-0000-0000-000000000001', 'https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=800&q=80', TRUE),
    ('c0000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000001', 'https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?auto=format&fit=crop&w=800&q=80', FALSE),
    ('c0000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000002', 'https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?auto=format&fit=crop&w=800&q=80', TRUE),
    ('c0000000-0000-0000-0000-000000000004', 'b0000000-0000-0000-0000-000000000003', 'https://images.unsplash.com/photo-1586023492125-27b2c045efd7?auto=format&fit=crop&w=800&q=80', TRUE),
    ('c0000000-0000-0000-0000-000000000005', 'b0000000-0000-0000-0000-000000000004', 'https://images.unsplash.com/photo-1598928506311-c55ded91a20c?auto=format&fit=crop&w=800&q=80', TRUE)
ON CONFLICT (id) DO NOTHING;

-- 8. Seed quan he tien ich cho phong mau
INSERT INTO room_amenities (room_id, amenity_id) VALUES
    ('b0000000-0000-0000-0000-000000000001', 1),
    ('b0000000-0000-0000-0000-000000000001', 2),
    ('b0000000-0000-0000-0000-000000000001', 3),
    ('b0000000-0000-0000-0000-000000000001', 4),
    ('b0000000-0000-0000-0000-000000000001', 7),
    ('b0000000-0000-0000-0000-000000000002', 1),
    ('b0000000-0000-0000-0000-000000000002', 2),
    ('b0000000-0000-0000-0000-000000000002', 3),
    ('b0000000-0000-0000-0000-000000000002', 8),
    ('b0000000-0000-0000-0000-000000000002', 9),
    ('b0000000-0000-0000-0000-000000000003', 1),
    ('b0000000-0000-0000-0000-000000000003', 4),
    ('b0000000-0000-0000-0000-000000000003', 7),
    ('b0000000-0000-0000-0000-000000000003', 10),
    ('b0000000-0000-0000-0000-000000000004', 1),
    ('b0000000-0000-0000-0000-000000000004', 2),
    ('b0000000-0000-0000-0000-000000000004', 3),
    ('b0000000-0000-0000-0000-000000000004', 8),
    ('b0000000-0000-0000-0000-000000000004', 10)
ON CONFLICT (room_id, amenity_id) DO NOTHING;
