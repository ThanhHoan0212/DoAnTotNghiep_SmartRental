package com.phongtro.backend.service;
import com.phongtro.backend.entity.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
public final class DepositFinalizer {
    private DepositFinalizer() {}
    public static void apply(Contract contract, Room room, String reference) {
        Instant now = Instant.now();
        contract.setDepositPaidAt(now);
        contract.setPaymentReference(reference);
        contract.setFormalizedAt(now);
        contract.setContractCode("HD-" + today().format(DateTimeFormatter.ofPattern("yyyyMM"))
                + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        contract.setDocumentContent("HỢP ĐỒNG THUÊ PHÒNG — BẢN GIẢ LẬP\n"
                + "Mã hợp đồng: " + contract.getContractCode() + "\n"
                + "Chủ phòng: " + contract.getLandlord().getFullName() + " (" + contract.getLandlord().getEmail() + ")\n"
                + "Người thuê: " + contract.getTenant().getFullName() + " (" + contract.getTenant().getEmail() + ")\n"
                + "Phòng: " + room.getTitle() + "\nĐịa chỉ: " + room.getAddress() + "\n"
                + "Thời hạn: " + contract.getStartDate() + " đến " + contract.getEndDate() + "\n"
                + "Tiền thuê mỗi tháng: " + contract.getMonthlyRent() + " VNĐ\n"
                + "Tiền cọc đã xác nhận: " + contract.getDepositAmount() + " VNĐ\n"
                + "Điều khoản bổ sung: " + (contract.getTerms() == null ? "Không có" : contract.getTerms()) + "\n"
                + "Hợp đồng có hiệu lực khi cả người thuê và chủ phòng hoàn tất ký giả lập.\n"
                + "Đây là môi trường mô phỏng: không thu tiền thật và không tạo chữ ký số được chứng thực.");
        contract.setStatus(ContractStatus.AWAITING_SIGNATURES);
    }
    private static LocalDate today() { return LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")); }
}
