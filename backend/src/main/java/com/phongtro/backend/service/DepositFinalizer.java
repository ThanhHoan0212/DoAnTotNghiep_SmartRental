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
        contract.setDocumentContent("CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM\n"
                + "Độc lập – Tự do – Hạnh phúc\n"
                + "--------------------------------------------------\n"
                + "HỢP ĐỒNG THUÊ PHÒNG TRỌ ĐIỆN TỬ\n"
                + "Mã hợp đồng: " + contract.getContractCode() + "\n\n"
                + "1. BÊN CHO THUÊ (BÊN A): " + contract.getLandlord().getFullName() + " - Email: " + contract.getLandlord().getEmail() + "\n"
                + "2. BÊN THUÊ (BÊN B): " + contract.getTenant().getFullName() + " - Email: " + contract.getTenant().getEmail() + "\n"
                + "3. TÀI SẢN THUÊ: " + room.getTitle() + " (" + room.getAddress() + ")\n"
                + "4. THỜI HẠN: Từ ngày " + contract.getStartDate() + " đến ngày " + contract.getEndDate() + "\n"
                + "5. TIỀN THUÊ: " + String.format("%,d", contract.getMonthlyRent().longValue()) + " VNĐ/tháng\n"
                + "6. TIỀN CỌC ĐÃ XÁC NHẬN: " + String.format("%,d", contract.getDepositAmount().longValue()) + " VNĐ (Đối soát VNPAY: " + reference + ")\n"
                + "7. THỎA THUẬN BỔ SUNG: " + (contract.getTerms() == null || contract.getTerms().isBlank() ? "Không có" : contract.getTerms()) + "\n\n"
                + "Hợp đồng được ký kết dưới dạng thông điệp dữ liệu điện tử có giá trị pháp lý theo Luật Giao dịch điện tử số 20/2023/QH15.\n"
                + "Hai bên xác nhận nội dung và tiến hành ký số thông qua Dịch vụ Chữ ký số từ xa VNPT SmartCA.");
        contract.setStatus(ContractStatus.AWAITING_SIGNATURES);
    }
    private static LocalDate today() { return LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")); }
}
