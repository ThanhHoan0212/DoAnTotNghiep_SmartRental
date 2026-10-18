# Hệ Thống Tìm Kiếm Phòng Trọ Thông Minh - Backend Service

> Đồ án tốt nghiệp: **"Phát triển hệ thống tìm kiếm phòng trọ thông minh: Tích hợp AI phát hiện gian lận và dự đoán xu hướng giá thuê"**

---

## 1. Công nghệ & Kiến trúc

- **Framework:** Spring Boot 3.3.3 (Java 17+)
- **Cơ sở dữ liệu:** PostgreSQL 15+
- **Quản lý Schema Migration:** Flyway (`src/main/resources/db/migration`)
- **Bảo mật:** Spring Security 6 + JJWT 0.12.6 (Access Token + Refresh Token trong Database)
- **Tài liệu API:** OpenAPI 3 / Swagger UI (`springdoc-openapi`)
- **Kiến trúc:** Layered Architecture (Controller → Service → Repository → Entity, DTO riêng biệt)
- **Containerization:** Docker & Docker Compose

---

## 2. Module 1: Quản lý Người dùng & Xác thực (User & Authentication)

### 2.1. Thiết kế Thực thể & Cơ sở Dữ liệu

1. **Bảng `users`**:
   - `id`: Khóa chính kiểu `UUID` (tạo tự động bằng `gen_random_uuid()` của PostgreSQL).
     - *Lý do dùng UUID thay vì số nguyên tự tăng:* Tránh lộ quy mô số lượng người dùng qua ID tuần tự, bảo mật hơn trước các cuộc tấn công quét ID, và thuận lợi cho việc tích hợp đồng bộ dữ liệu giữa Spring Boot và AI Service (Python FastAPI).
   - `email`: Duy nhất (`UNIQUE`), bắt buộc, dùng làm tên đăng nhập.
   - `password_hash`: Chuỗi mật khẩu băm theo thuật toán BCrypt với cost factor 10.
   - `full_name`: Họ và tên hiển thị.
   - `phone`: Số điện thoại duy nhất (`UNIQUE`).
   - `role`: Phân quyền người dùng (`TENANT`, `LANDLORD`, `ADMIN`).
   - `status`: Trạng thái tài khoản (`ACTIVE`, `INACTIVE`, `BANNED`).
   - `created_at`, `updated_at`: Được kiểm soát tự động bởi JPA Auditing.

2. **Bảng `refresh_tokens`**:
   - `id`: UUID.
   - `user_id`: Khóa ngoại trỏ đến `users(id)` với ràng buộc `ON DELETE CASCADE`.
   - `token`: Chuỗi token ngẫu nhiên dạng UUID duy nhất.
   - `expiry_date`: Thời điểm hết hạn (mặc định 7 ngày).
   - `revoked`: Cờ đánh dấu token đã bị thu hồi (khi người dùng đăng xuất hoặc đổi mật khẩu).
   - *Lý do lưu Refresh Token trong DB:* Cho phép Admin hoặc chính người dùng hủy phiên đăng nhập từ xa, hỗ trợ cơ chế Token Rotation ngăn chặn kẻ gian đánh cắp token.

---

## 3. Hướng dẫn Chạy Hệ thống

### Chạy trực tiếp trên Windows

Từ thư mục `backend`, chạy `./mvnw.cmd spring-boot:run` với JDK 17+ và PostgreSQL đang hoạt động. `JAVA_HOME` phải trỏ tới thư mục JDK chứa `bin/java.exe`. Trong `.env`, đặt chú thích trên dòng riêng, không gắn vào các giá trị số như `JWT_EXPIRATION_MS`.

Migration chữ ký SmartCA dùng V17 để tránh trùng V15/V16 trong database local. Profile `dev` cho phép giữ lịch sử migration không có trên nhánh hiện tại; checksum của các migration hiện có vẫn được kiểm tra. Khi triển khai `prod`, cần đầy đủ file migration tương ứng với database đích.

### Cách 1: Chạy Database bằng Docker Compose (Khuyên dùng khi dev)

1. Khởi động PostgreSQL 15:
   ```bash
   docker-compose up -d postgres
   ```
2. Chạy ứng dụng Spring Boot từ IDE (IntelliJ IDEA / VS Code / Eclipse):
   - Mở class `PhongTroBackendApplication.java` và chọn **Run**.
   - Flyway sẽ tự động chạy file migration `V1__init_users_and_tokens.sql` để khởi tạo bảng và tài khoản Admin mẫu.

### Cách 2: Chạy toàn bộ hệ thống bằng Docker Compose

```bash
docker-compose up --build
```

---

## 4. Tài khoản Mặc định Khởi tạo

Khi khởi động lần đầu, Flyway sẽ tự động tạo tài khoản Quản trị viên (Admin) để kiểm thử:
- **Email:** `admin@phongtro.vn`
- **Mật khẩu:** `Admin@123`
- **Vai trò:** `ADMIN`

---

## 5. Danh sách API Module 1 (Swagger UI)

Sau khi ứng dụng khởi chạy thành công, truy cập Swagger UI tại:
👉 **`http://localhost:8080/swagger-ui/index.html`**

| Phương thức | Endpoint | Phân quyền | Mô tả |
|---|---|---|---|
| `POST` | `/api/v1/auth/register` | Public | Đăng ký tài khoản người dùng mới (`TENANT` / `LANDLORD`) |
| `POST` | `/api/v1/auth/login` | Public | Đăng nhập tài khoản, nhận Access Token + Refresh Token |
| `POST` | `/api/v1/auth/refresh-token` | Public | Cấp mới Access Token bằng Refresh Token |
| `POST` | `/api/v1/auth/logout` | Authenticated | Thu hồi Refresh Token để đăng xuất |
| `POST` | `/api/v1/auth/forgot-password` | Public | Gửi mã xác nhận khôi phục mật khẩu |
| `POST` | `/api/v1/auth/reset-password` | Public | Đặt lại mật khẩu mới với mã xác nhận |
| `GET` | `/api/v1/users/me` | Authenticated | Xem hồ sơ cá nhân |
| `PUT` | `/api/v1/users/me` | Authenticated | Cập nhật họ tên, số điện thoại, avatar |
| `PUT` | `/api/v1/users/me/change-password` | Authenticated | Đổi mật khẩu |
| `GET` | `/api/v1/users` | `ADMIN` | Phân trang danh sách người dùng (lọc theo role, status) |
| `PATCH` | `/api/v1/users/{id}/status` | `ADMIN` | Khóa hoặc kích hoạt lại tài khoản người dùng |

## 6. Quản trị và hỗ trợ hợp đồng

Sau khi đăng nhập bằng role `ADMIN`, frontend tự chuyển đến `/admin`: quản lý phòng, tài khoản, xử lý khiếu nại và báo cáo phân tích. API `/api/v1/admin/**` chỉ dành cho admin.

- Phòng: lọc, xem/sửa, duyệt, từ chối, ẩn và xóa phòng chưa phát sinh hợp đồng. Trạng thái giữ chỗ/đã thuê được kiểm soát qua hợp đồng.
- Tài khoản: tìm kiếm theo họ tên/email/điện thoại, lọc theo vai trò/trạng thái, khóa/mở khóa. Không cho phép khóa admin; tài khoản bị khóa không thể tiếp tục dùng JWT cũ.
- Hỗ trợ: vào **Hợp đồng → Hỗ trợ hợp đồng**. Chỉ người thuê/chủ nhà của hợp đồng được gửi yêu cầu; mỗi bên chỉ xem yêu cầu do mình gửi. Admin xem mã hợp đồng, phòng liên quan và phản hồi tại `/admin/complaints`. Đường dẫn cũ `/support` chuyển đến `/contracts`.
- Báo cáo: hiện trạng hệ thống, tăng trưởng theo tháng, cơ cấu tài khoản/tin đăng, nguồn cung và giá trung bình theo quận, hợp đồng tạo trong kỳ, tiền cọc VNPay thành công và xuất CSV. Tiền cọc là tiền thu hộ, không phải doanh thu nền tảng.

`POST /api/v1/complaints` yêu cầu `subject`, `category`, `description`, `contractId`. Backend kiểm tra người gửi thuộc hợp đồng và tự lấy phòng tương ứng. `GET /api/v1/complaints/mine?contractId=<uuid>` lọc yêu cầu của người đăng nhập theo hợp đồng. `PATCH /api/v1/admin/complaints/{id}` dùng `version` để tránh ghi đè khi xử lý đồng thời.

Migration V18 được giữ nguyên lịch sử đã áp dụng. V19 bổ sung liên kết hợp đồng cho khiếu nại; các bản ghi cũ vẫn được giữ lại. Chức năng tin tức đã gỡ.

Báo cáo: `GET /api/v1/admin/analytics?from=YYYY-MM-DD&to=YYYY-MM-DD`, tính đến hết ngày cuối theo giờ Việt Nam, mặc định 6 tháng, tối đa 3 năm. Các chỉ số hiện trạng và phân bố phòng/tài khoản không phụ thuộc bộ lọc ngày.

Kiểm tra backend: `./mvnw.cmd test`. Kiểm tra frontend: `npm run build` từ thư mục `frontend`.
