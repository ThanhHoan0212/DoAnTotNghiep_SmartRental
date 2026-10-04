package com.phongtro.backend.entity;

public enum ContractStatus {
    PENDING,    // Yêu cầu thuê mới gửi, chờ chủ nhà phê duyệt
    AWAITING_DEPOSIT,
    AWAITING_SIGNATURES,
    ACTIVE,     // Hai bên đã ký, hợp đồng có hiệu lực
    REJECTED,   // Chủ nhà từ chối yêu cầu thuê
    EXPIRED,    // Hợp đồng đã đến ngày kết thúc và hết hiệu lực
    TERMINATED, // Hai bên thỏa thuận thanh lý hoặc chấm dứt trước hạn
    CANCELLED   // Người thuê tự hủy yêu cầu khi chưa được duyệt
}
