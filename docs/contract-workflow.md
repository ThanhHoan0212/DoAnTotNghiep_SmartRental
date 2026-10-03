# Luồng thuê phòng, hủy yêu cầu và chấm dứt trước hạn

## Thuê phòng

1. Người thuê gửi yêu cầu `PENDING`, mã `YC-...`.
2. Chủ phòng chấp nhận: `AWAITING_DEPOSIT`, phòng `RESERVED`, hạn cọc 24 giờ.
3. Thanh toán cọc giả lập: lưu thời điểm và mã `SIM-PAY-...`, tạo văn bản cố định, chuyển `AWAITING_SIGNATURES`.
4. Hai bên đọc và ký giả lập bằng tài khoản riêng. Đủ hai chữ ký: `ACTIVE`, phòng `RENTED`.

## Chưa có hiệu lực

- Chưa trả cọc (`PENDING` / `AWAITING_DEPOSIT`): người thuê nhập lý do và xác nhận hủy. Hệ thống chuyển `CANCELLED`; nếu đang giữ phòng thì mở lại phòng. Hủy một yêu cầu chưa được duyệt không mở phòng đang thuộc yêu cầu khác.
- Đã trả cọc, đang chờ ký: một bên tạo **yêu cầu hủy**, nêu lý do và đề xuất xử lý tiền cọc. Yêu cầu có trạng thái riêng `PENDING`; hợp đồng vẫn `AWAITING_SIGNATURES`, phòng vẫn `RESERVED`.
- Tạm dừng ký trong lúc chờ xác nhận hủy để tránh hợp đồng phát sinh hiệu lực giữa lúc đang thỏa thuận.
- Chỉ bên còn lại được đồng ý hoặc từ chối. Đồng ý: hợp đồng `CANCELLED`, phòng được mở lại. Từ chối: tiếp tục chờ ký. Lịch sử yêu cầu và phản hồi được giữ lại.

## Đang có hiệu lực

- Nút **Yêu cầu chấm dứt hợp đồng** mở phần đề xuất trong chi tiết hợp đồng, không chấm dứt ngay.
- Người gửi nhập lý do, ngày muốn kết thúc và phương án cọc. Ngày phải không trước hôm nay/ngày bắt đầu thuê và phải trước ngày hết hạn gốc.
- Bên còn lại xác nhận toàn bộ đề xuất (bao gồm phương án cọc), hoặc từ chối kèm lý do. Không được tự duyệt hoặc duyệt thay bằng tài khoản admin không thuộc hợp đồng.
- Khi được chấp nhận: lưu `agreedEndDate` riêng, giữ nguyên ngày hết hạn gốc và văn bản đã ký. Hợp đồng vẫn `ACTIVE`, phòng vẫn `RENTED` cho tới ngày thống nhất.
- Từ ngày đã thống nhất, theo múi giờ `Asia/Ho_Chi_Minh`, tác vụ chạy mỗi phút chuyển `TERMINATED`, ghi `terminatedAt`, hoàn tất yêu cầu và mở phòng. Nếu xác nhận ngày hôm nay thì xử lý ngay khi xác nhận. Nếu dịch vụ ngừng chạy, tác vụ xử lý bù khi khởi động lại.
- Nếu từ chối, hợp đồng tiếp tục hiệu lực. Có thể gửi đề xuất mới; lịch sử cũ không bị ghi đè.
- Yêu cầu chưa được duyệt không ngăn hợp đồng hết hạn tự nhiên. Khi hết hạn, yêu cầu còn chờ chuyển `LAPSED`.

## Tiền cọc

Mỗi đề xuất ghi `FULL` (hoàn toàn bộ), `PARTIAL` (hoàn một phần), hoặc `NONE` (không hoàn), số tiền hoàn và căn cứ thỏa thuận. Số tiền phải khớp loại đề xuất và không vượt tiền cọc. Bên còn lại đồng ý đúng phương án này; nếu chưa thống nhất thì từ chối và gửi đề xuất mới.

Đây là **ghi nhận thỏa thuận**, không tự động chuyển tiền và không đánh dấu tiền đã hoàn. Lịch sử thanh toán cọc giả lập ban đầu được giữ nguyên. Việc xác nhận hoàn tiền thực tế chưa được tích hợp.

## API và bảo vệ dữ liệu

- `PATCH /api/v1/contracts/{id}/status`: duyệt mở cọc, từ chối, hủy chưa trả cọc. Cấm chuyển trực tiếp `TERMINATED` (kể cả admin).
- `POST /api/v1/contracts/{id}/closure-requests`: `{ reason, requestedEndDate?, refundType, refundAmount, settlementNote }`. Loại yêu cầu được suy ra từ trạng thái hợp đồng.
- `POST /api/v1/contracts/{id}/closure-requests/{requestId}/response`: `{ accepted, reason? }`; từ chối bắt buộc có lý do.
- Chi tiết và danh sách hợp đồng trả `closureRequests`, `agreedEndDate`, `terminatedAt`. Giao diện đồng bộ nền mỗi 5 giây/khi quay lại tab; yêu cầu mới hiển thị trên thẻ hợp đồng và phần chi tiết.
- Một yêu cầu đang chờ/đã được duyệt tối đa trên mỗi hợp đồng. Version hợp đồng và unique index bảo vệ cập nhật đồng thời. API chặn phản hồi lặp hoặc phản hồi sai bên; khi xung đột cần tải lại trạng thái.
- Phòng chỉ mở lại trong giao dịch hoàn tất hủy/chấm dứt, không khi gửi yêu cầu hoặc chấp nhận ngày kết thúc tương lai. Phòng đang bị ẩn không tự mở lại.

Khởi động lại backend để Flyway áp dụng migration V12 (và V11 nếu chưa áp dụng). Không sửa dữ liệu lịch sử để tự tạo chữ ký, thanh toán hoặc thỏa thuận chấm dứt.
