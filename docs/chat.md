# Chat giữa các tài khoản

- Mọi vai trò (người thuê, chủ nhà, quản trị viên) đều có thể chat với nhau.
- Chỉ bắt đầu liên hệ bằng cách mở chi tiết tin phòng và chọn **Nhắn tin cho chủ nhà**, áp dụng cho mọi vai trò.
- Mở **Tin nhắn** trên thanh điều hướng (`/chat`) để xem và trả lời các cuộc trò chuyện đã có.
- Không có tìm kiếm tài khoản hoặc API tạo cuộc trò chuyện theo ID tài khoản. Các cuộc trò chuyện trực tiếp đã tồn tại vẫn được giữ để xem và trả lời.
- Một cuộc trò chuyện cho mỗi cặp phòng/khách thuê; mở lại sẽ dùng lịch sử cũ.
- Tin mới cập nhật mỗi 3 giây khi mở cuộc trò chuyện, danh sách cập nhật mỗi 5 giây.
- Không giới hạn 2.000 ký tự mỗi tin; ô soạn tự giãn đến 100px rồi cuộn bên trong. Lịch sử tải 50 tin mỗi trang.
- Chỉ hai thành viên được đọc/gửi; tài khoản bị khóa không thể gửi hoặc nhận tin mới.
- Gửi ảnh JPG, PNG hoặc GIF tối đa 5 MB, 20 triệu điểm ảnh; chọn ảnh, xem trước và gửi kèm chú thích tùy chọn. Nhấn ảnh để xem lớn.
- Nút video gửi tệp MP4 tối đa 25 MB; khuyến nghị H.264/AAC để phát trong trình duyệt. Có xem trước, lời nhắn tùy chọn, nút phát và tải xuống. Video chỉ được tải về khi nhấn xem.
- Ảnh và video dùng chung một nút đính kèm trong ô soạn, tự phân loại tệp sau khi chọn.
- Người gửi có thể thu hồi tin nhắn của mình (văn bản, ảnh, video). Tin nhắn đổi thành **Tin nhắn đã được thu hồi**, cảm xúc bị xóa và tệp đính kèm không còn tải được qua API. Không thể xóa bản sao mà người nhận đã tải trước đó.
- Hai bên có thể chọn một cảm xúc mỗi người trên một tin nhắn: 👍 ❤️ 😂 😮 😢 😡. Chọn lại cùng cảm xúc để bỏ, chọn cảm xúc khác để đổi. Tin đã thu hồi không thể thả cảm xúc.
- Migration V26 thêm trạng thái thu hồi, phiên bản tin nhắn và bảng cảm xúc. Trạng thái được đồng bộ khi cập nhật lịch sử, bao gồm các trang lịch sử cũ đã mở.
- Mục **Ảnh và video** liệt kê cả hai loại tệp. Migration V25 thêm cờ video và chỉ mục thư viện; dữ liệu video lưu cùng kho tệp riêng trong PostgreSQL.
- Nút **Ảnh đã chia sẻ** ở đầu khung chat mở thư viện ảnh của hai bên, mới nhất trước; chọn **Xem thêm ảnh cũ** để tải tiếp 20 ảnh mỗi lần, không phụ thuộc lịch sử tin nhắn đang mở.
- Ảnh lưu trong PostgreSQL, chỉ tải qua API có JWT và kiểm tra người tham gia. Migration V23 tạo bảng ảnh và cờ ảnh cho tin nhắn.
- Phòng có trò chuyện không thể xóa; chủ nhà có thể ẩn tin để giữ lịch sử.

## Cơ sở dữ liệu

Flyway tự chạy `V20__create_chat.sql` và `V21__allow_direct_chat.sql` khi khởi động backend mới, tạo
`chat_conversations` và `chat_messages` và cho phép chat không gắn phòng. Lịch sử cũ được giữ nguyên. Cần build và khởi động lại backend
để sử dụng chức năng này.

## API (yêu cầu JWT)

| Method | Đường dẫn | Chức năng |
| --- | --- | --- |
| GET | `/api/v1/chat/conversations` | Danh sách cuộc trò chuyện của tài khoản |
| POST | `/api/v1/chat/rooms/{roomId}` | Mở cuộc trò chuyện với chủ phòng, mọi vai trò |
| GET | `/api/v1/chat/conversations/{id}/messages?page=0` | Lịch sử; trang 0 là 50 tin mới nhất, trả theo thứ tự thời gian tăng dần |
| POST | `/api/v1/chat/conversations/{id}/messages` | Gửi JSON `{ "content": "Xin chào" }` |
| POST | `/api/v1/chat/conversations/{id}/images` | Multipart `image` và `caption` tùy chọn; gửi ảnh và tạo tin nhắn trong cùng giao dịch |
| GET | `/api/v1/chat/conversations/{id}/images?page=0` | Danh sách ảnh đã chia sẻ, mới nhất trước, 20 ảnh/trang, gồm cờ `hasMore` |
| GET | `/api/v1/chat/conversations/{id}/messages/{messageId}/image` | Trả loại ảnh và dữ liệu base64 sau khi kiểm tra quyền, không lưu cache công khai |
| POST | `/api/v1/chat/conversations/{id}/videos` | Multipart `video` MP4 tối đa 25 MB, `caption` tùy chọn |
| GET | `/api/v1/chat/conversations/{id}/messages/{messageId}/video` | Dữ liệu video có kiểm tra quyền, hỗ trợ HTTP Range, không lưu cache công khai |
| GET | `/api/v1/chat/conversations/{id}/media?page=0` | Thư viện ảnh và video của cuộc trò chuyện, 20 tệp/trang |
| POST | `/api/v1/chat/conversations/{id}/messages/{messageId}/recall` | Thu hồi tin nhắn của tài khoản hiện tại |
| POST | `/api/v1/chat/conversations/{id}/messages/{messageId}/reaction` | JSON `{ "emoji": "👍" }`, thêm/đổi/bỏ cảm xúc của tài khoản hiện tại |
