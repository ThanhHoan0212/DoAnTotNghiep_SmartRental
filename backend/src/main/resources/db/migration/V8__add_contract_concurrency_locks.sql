-- V8: Ngăn chặn triệt để xung đột bất đồng bộ và Double-Booking khi nhiều người cùng ký/thuê 1 phòng
-- Tạo Partial Unique Index: Ở mọi thời điểm, mỗi phòng trọ chỉ có DUY NHẤT 1 hợp đồng ở trạng thái ACTIVE

CREATE UNIQUE INDEX IF NOT EXISTS uk_room_single_active_contract
    ON contracts (room_id)
    WHERE status = 'ACTIVE';
