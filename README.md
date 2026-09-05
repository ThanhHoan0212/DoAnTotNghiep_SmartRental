# Hệ Thống Tìm Kiếm Phòng Trọ Thông Minh (Smart Rental System)

> Đồ án Tốt nghiệp: **"Phát triển hệ thống tìm kiếm phòng trọ thông minh: Tích hợp AI phát hiện gian lận và dự đoán xu hướng giá thuê"**

---

## 1. Cấu Trúc Dự Án

- **`backend/`**: Spring Boot 3.3.3 + PostgreSQL 15 + Spring Security + JWT + Flyway.
- **`frontend/`**: React 19 + TypeScript + Vite.

---

## 2. Các Module Đã Hoàn Thành

### Module 1: Quản lý Người dùng & Xác thực (User & Authentication)
- Đăng ký tài khoản (Người thuê `TENANT`, Chủ nhà `LANDLORD`).
- Đăng nhập JWT (Access Token + Refresh Token lưu database, cơ chế thu hồi token).
- Quên mật khẩu, đổi mật khẩu, xem và cập nhật hồ sơ cá nhân.
- Admin quản lý và khóa/mở khóa tài khoản người dùng.

### Module 2: Quản lý Tin đăng Phòng trọ & Tiện ích (RoomListing & Amenity)
- Danh mục tiện ích phòng trọ (`Amenity`): Wifi, Điều hòa, Nóng lạnh, Chỗ để xe, v.v.
- Tin đăng phòng trọ (`Room`): Tiêu đề, mô tả, giá thuê, diện tích, địa chỉ, quận huyện, tọa độ, lượt xem.
- Hình ảnh phòng trọ (`RoomImage`): Hỗ trợ nhiều ảnh cho mỗi phòng trọ, chỉ định ảnh đại diện chính.
- Bảng trung gian Many-to-Many `room_amenities` chuẩn 3NF kết nối Phòng trọ và Tiện ích.
- CRUD tin đăng, tự động tăng `view_count` khi xem chi tiết.
- Phân quyền: Chủ nhà và Admin được đăng tin (`POST /api/v1/rooms`), chỉnh sửa và xóa tin của mình; Admin có quyền kiểm duyệt tin (`APPROVED`, `REJECTED`, `HIDDEN`).
- Giao diện Frontend: Trang danh sách phòng trọ kèm bộ lọc (quận huyện, giá, tiện ích), trang chi tiết phòng kèm gallery và thông tin chủ nhà, trang đăng tin phòng mới (`/post-room`).

---

## 3. Hướng Dẫn Khởi Chạy Hệ Thống

### Bước 1: Khởi động Cơ sở dữ liệu PostgreSQL
```bash
cd backend
docker-compose up -d postgres
```

### Bước 2: Chạy Backend Spring Boot (Cổng 8080)
```bash
cd backend
java -jar target/smart-rental-backend-0.0.1-SNAPSHOT.jar
```
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`

### Bước 3: Chạy Frontend React (Cổng 5173)
```bash
cd frontend
npm run dev
```
- Truy cập Web App tại: `http://localhost:5173`

---

## 4. Tài Khoản Demo

| Vai trò | Email | Mật khẩu | Ghi chú |
|---|---|---|---|
| **Quản trị viên (Admin)** | `admin@phongtro.vn` | `Admin@123` | Toàn quyền duyệt tin, xem thống kê |
| **Chủ nhà (Landlord)** | Tự đăng ký qua `/register` (chọn vai trò "Chủ nhà") | Mật khẩu tự đặt | Đăng tin, sửa/xóa tin của mình |
| **Người thuê (Tenant)** | Tự đăng ký qua `/register` (chọn vai trò "Người thuê") | Mật khẩu tự đặt | Tìm kiếm, xem chi tiết phòng trọ |
