# Thanh toán cọc qua VNPAY Sandbox

Chức năng áp dụng cho tiền cọc của yêu cầu thuê đã được chủ nhà chấp nhận (`AWAITING_DEPOSIT`). Số tiền lấy từ hợp đồng ở backend, không nhận số tiền từ trình duyệt. Thanh toán tiền thuê hàng tháng và hoàn tiền tự động chưa nằm trong luồng này.

## Test nhanh trên localhost (không cần ngrok cho querydr)

1. Điền secret VNPAY cấp trong `backend/.env`, tương ứng TmnCode `3IINFGVH`.
2. Dừng backend cũ. Trong PowerShell, vào thư mục `backend` và chạy `powershell -ExecutionPolicy Bypass -File .\start-local.ps1`. Script nạp biến VNPAY từ `.env`, không in secret.
3. Chạy `npm run dev` ở `frontend` và mở `http://localhost:5173`.
4. Người thuê chọn hợp đồng chờ cọc, bấm thanh toán, chọn NCB; dùng thẻ `9704198526191432198`, tên `NGUYEN VAN A`, ngày phát hành `07/15`, OTP `123456`.
5. Khi quay về, frontend gọi `POST /api/v1/payments/vnpay/{reference}/reconcile`. Backend gọi API querydr của VNPAY, xác thực chữ ký phản hồi, merchant, mã đơn, số tiền, loại và trạng thái giao dịch rồi cập nhật hợp đồng.
6. Nếu đã thanh toán trước bản cập nhật, mở lại trang kết quả cũ và bấm kiểm tra lại, không cần trả cọc lần nữa.

`VNPAY_IPN_URL` có thể để trống khi chỉ test bằng querydr. API querydr gọi từ backend ra VNPAY, không yêu cầu VNPAY truy cập localhost. IPN vẫn là cơ chế thông báo tự động theo yêu cầu tích hợp của VNPAY; cần cấu hình trước khi triển khai đầy đủ.

VNPAY giới hạn truy vấn lặp trong 5 phút. Backend lưu `last_queried_at` và khóa bản ghi khi đặt lượt truy vấn, sau đó nhả transaction trước khi gọi mạng. Frontend chỉ đối soát ở lần mở trang/kiểm tra lại; các lần thăm dò tiếp theo chỉ đọc database. Không tìm thấy giao dịch, lỗi mạng, kết quả chờ xử lý hoặc chữ ký sai đều không ghi nhận đã trả cọc. Trang hiển thị thời điểm có thể truy vấn lại.

Nếu đóng trang trước khi xác nhận và chưa đăng ký IPN, cần mở lại trang kết quả để đối soát; bản này chưa có tác vụ đối soát nền tự động.

## Cấu hình

Điền trong `backend/.env` (file này được Git bỏ qua):

```dotenv
VNPAY_TMN_CODE=3IINFGVH
VNPAY_HASH_SECRET=<secret sandbox thật do VNPAY cấp>
VNPAY_PAY_URL=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
VNPAY_RETURN_URL=http://localhost:5173/payment/vnpay-return
VNPAY_IPN_URL=
VNPAY_QUERY_URL=https://sandbox.vnpayment.vn/merchant_webapi/api/transaction
VNPAY_QUERY_IP=127.0.0.1
```

Không đưa secret vào frontend hoặc commit lên Git. Không sử dụng nguyên giá trị mẫu trong dấu `<...>`.

Spring Boot không tự đọc `.env` khi chạy trực tiếp. Từ thư mục `backend`, nạp riêng các biến VNPAY trước khi chạy Maven:

```powershell
Get-Content .env | ForEach-Object {
    if ($_ -match '^(VNPAY_[A-Z_]+)=(.*)$') {
        [Environment]::SetEnvironmentVariable($matches[1], $matches[2].Trim(), 'Process')
    }
}
.\mvn.ps1 spring-boot:run
```

Nếu chạy bằng IDE, đặt các biến trên vào Run Configuration. Docker Compose đã được bổ sung truyền các biến VNPAY từ `.env` vào container backend.

Đưa backend cổng 8080 ra một URL HTTPS công khai bằng hosting hoặc tunnel, rồi **đăng ký URL IPN với VNPAY**. Chỉ đặt `VNPAY_IPN_URL` trong `.env` không tự đăng ký endpoint; URL này không phải tham số gửi trong link thanh toán. Không dùng localhost làm IPN. Return URL localhost chỉ dùng khi trình duyệt khách hàng chạy trên chính máy phát triển.

## Luồng và API

1. Người thuê gửi yêu cầu, chủ nhà chấp nhận để mở cọc trong 24 giờ.
2. Người thuê bấm **Thanh toán cọc qua VNPAY**. `POST /api/v1/payments/vnpay/contracts/{contractId}` yêu cầu JWT của đúng người thuê, lưu giao dịch rồi trả URL ký HMAC-SHA512. Link còn hiệu lực được tái sử dụng khi bấm lại; link tối đa 15 phút, không vượt hạn cọc.
3. VNPAY gọi `GET /api/payments/vnpay/ipn` (cũng hỗ trợ `/api/v1/payments/vnpay/ipn`). Endpoint không yêu cầu JWT; kiểm tra chữ ký, mã merchant, mã giao dịch và số tiền. Tham số trùng tên bị từ chối. IPN lặp trả `02`, sai chữ ký `97`, sai số tiền `04`, không tìm thấy `01`, lỗi giao dịch DB trả `99` để VNPAY thử lại.
4. IPN hợp lệ với cả `vnp_ResponseCode=00` và `vnp_TransactionStatus=00` sẽ ghi nhận cọc, tạo văn bản và chuyển hợp đồng sang `AWAITING_SIGNATURES`. Endpoint cọc giả lập cũ đã bị vô hiệu hóa. Luồng ký hiện tại vẫn là ký giả lập.
5. Trang `/payment/vnpay-return` lấy mã tham chiếu, đọc `GET /api/v1/payments/vnpay/{reference}` bằng JWT của người thuê và thăm dò tối đa khoảng một phút. Trang không tin mã thành công trên query string. Nó yêu cầu backend truy vấn VNPAY bằng mã tham chiếu và thời gian tạo gốc đã lưu; chỉ kết quả đã được backend xác thực mới cập nhật trạng thái. Có thể bấm kiểm tra lại nếu IPN chậm.

Giao dịch thành công đến sau khi hợp đồng đã hủy/hết hạn, đã nhận một giao dịch khác, phòng đã đổi trạng thái hoặc tiền cọc đã thay đổi được lưu `REVIEW_REQUIRED`. Không khôi phục phòng đã giải phóng, không tự hoàn tiền. Quản trị viên cần đối soát và xử lý với VNPAY. Nếu link hết hạn mà IPN chưa tới, lần thử tiếp theo có mã mới; mọi khoản thanh toán thành công thừa được lưu để đối soát.

Tra cứu các trường hợp cần xử lý bằng tài khoản DB quản trị:

```sql
SELECT reference, contract_id, amount, status, transaction_no, completed_at
FROM vnpay_payments
WHERE status = 'REVIEW_REQUIRED'
ORDER BY completed_at;
```

Khi dùng reverse proxy, cấu hình trusted proxy để servlet nhận đúng IP khách hàng; backend không tự tin cậy header `X-Forwarded-For` do client gửi.

## Kiểm thử

- Chạy migration V13 và V14 qua Flyway khi backend khởi động.
- Thử bằng tài khoản/thẻ sandbox trong [tài liệu chính thức VNPAY](https://sandbox.vnpayment.vn/apis/docs/thanh-toan-pay/pay.html); kiểm tra thành công, hủy và IPN đến chậm.
- Unit test `VnpayServiceTest` kiểm tra ký HMAC, sai chữ ký/số tiền/merchant, quyền truy cập, giao dịch trùng, thất bại, quá hạn và tạo URL.
- Đã gọi querydr thật cho giao dịch sandbox hiện có: phản hồi API `00`, trạng thái giao dịch `00`, chữ ký hợp lệ. Kiểm tra này chỉ đọc kết quả, không thay đổi database. Chưa thử lại trọn vòng trình duyệt với bản backend mới. Đây là tích hợp sandbox; không chuyển sang thu tiền thật chỉ bằng đổi URL.
- Tài liệu querydr: https://sandbox.vnpayment.vn/apis/docs/truy-van-hoan-tien/querydr%26refund.html
