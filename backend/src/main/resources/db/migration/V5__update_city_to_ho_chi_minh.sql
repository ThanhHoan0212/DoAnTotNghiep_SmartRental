-- ==============================================================================
-- Flyway Migration V5: Chuyển đổi dữ liệu và mặc định tỉnh/thành phố sang TP. Hồ Chí Minh
-- ==============================================================================

-- 1. Cập nhật 4 phòng trọ mẫu sang địa chỉ và quận huyện TP. Hồ Chí Minh
UPDATE rooms
SET
    title = 'Phòng trọ cao cấp full nội thất gần ĐH HUTECH Bình Thạnh',
    description = 'Phòng mới xây 100%, đầy đủ điều hòa, nóng lạnh, giường tủ gỗ sang trọng. Giờ giấc tự do, không chung chủ, an ninh camera 24/7. Vị trí trung tâm thuận tiện đi lại ra Điện Biên Phủ, Xô Viết Nghệ Tĩnh, gần ngã tư Hàng Xanh.',
    address = 'Số 123 Điện Biên Phủ',
    ward = 'Phường 15',
    district = 'Bình Thạnh',
    city = 'Hồ Chí Minh',
    latitude = 10.7988,
    longitude = 106.7118
WHERE id = 'b0000000-0000-0000-0000-000000000001';

UPDATE rooms
SET
    title = 'Căn hộ studio ban công thoáng mát trung tâm Quận 1',
    description = 'Căn hộ studio có ban công view đẹp, ánh sáng tự nhiên ngập tràn. Trang bị bếp từ hút mùi riêng biệt, máy giặt riêng trong phòng. Khu dân trí cao, yên tĩnh, gần phố đi bộ Nguyễn Huệ, chợ Bến Thành.',
    address = '45 Nguyễn Thị Minh Khai',
    ward = 'Bến Nghé',
    district = 'Quận 1',
    city = 'Hồ Chí Minh',
    latitude = 10.7828,
    longitude = 106.6983
WHERE id = 'b0000000-0000-0000-0000-000000000002';

UPDATE rooms
SET
    title = 'Phòng khép kín giá rẻ cho sinh viên Làng Đại Học Thủ Đức',
    description = 'Phòng rộng rãi sạch sẽ, vệ sinh khép kín, có gác lửng để đồ hoặc ngủ. Gần chợ dân sinh, siêu thị, cách bến xe Miền Đông mới và trạm Metro 1km. Giá điện nước bình dân, chỗ để xe rộng rãi vân tay an toàn.',
    address = 'Số 45 Võ Văn Ngân',
    ward = 'Linh Chiểu',
    district = 'Thủ Đức',
    city = 'Hồ Chí Minh',
    latitude = 10.8505,
    longitude = 106.7628
WHERE id = 'b0000000-0000-0000-0000-000000000003';

UPDATE rooms
SET
    title = 'Phòng trọ mới tinh ban công ngập nắng gần ĐH Công Nghiệp Gò Vấp',
    description = 'Phòng trọ khép kín mới 100%, có thang máy di chuyển, bảo vệ trực ngày đêm. Đầy đủ điều hòa Inverter tiết kiệm điện, bình nóng lạnh, kệ bếp nấu ăn xinh xắn. Vị trí gần ĐH Công Nghiệp TP.HCM, sân bay Tân Sơn Nhất.',
    address = 'Số 88 Quang Trung',
    ward = 'Phường 10',
    district = 'Gò Vấp',
    city = 'Hồ Chí Minh',
    latitude = 10.8354,
    longitude = 106.6667
WHERE id = 'b0000000-0000-0000-0000-000000000004';

-- 2. Cập nhật tất cả các phòng khác nếu còn để giá trị 'Hà Nội'
UPDATE rooms
SET city = 'Hồ Chí Minh'
WHERE city = 'Hà Nội';

-- 3. Đặt giá trị mặc định cho cột city là 'Hồ Chí Minh'
ALTER TABLE rooms ALTER COLUMN city SET DEFAULT 'Hồ Chí Minh';
