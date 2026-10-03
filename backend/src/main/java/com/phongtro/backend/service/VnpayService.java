package com.phongtro.backend.service;

import com.phongtro.backend.config.VnpayProperties;
import com.phongtro.backend.entity.*;
import com.phongtro.backend.exception.*;
import com.phongtro.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class VnpayService {

    private final VnpayProperties config;
    private final VnpayPaymentRepository payments;
    private final ContractRepository contracts;
    private final RoomRepository rooms;

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZONE);

    public record PaymentView(
            String reference,
            UUID contractId,
            String status,
            long amount,
            String paymentUrl,
            Instant expiresAt,
            Instant nextQueryAt
    ) {}

    public record QueryTarget(
            String reference,
            String transactionDate
    ) {}

    @Transactional
    public QueryTarget reserveQuery(String reference, String email) {

        if (config.getHashSecret() == null
                || config.getHashSecret().isBlank()) {

            throw error("Chưa cấu hình VNPAY_HASH_SECRET.");
        }

        VnpayPayment payment =
                payments.findLockedByReference(reference)
                        .orElseThrow(() ->
                                new AppException(
                                        ErrorCode.RESOURCE_NOT_FOUND
                                )
                        );

        requireTenant(payment.getContract(), email);

        Instant now = Instant.now();

        if (!"PENDING".equals(payment.getStatus())
                || (payment.getLastQueriedAt() != null
                && payment.getLastQueriedAt()
                .plusSeconds(300)
                .isAfter(now))) {

            return null;
        }

        // Read the exact original timestamp,
        // including for payments created before querydr support.
        String date =
                Arrays.stream(
                                java.net.URI
                                        .create(payment.getPaymentUrl())
                                        .getRawQuery()
                                        .split("&")
                        )
                        .filter(p ->
                                p.startsWith(
                                        "vnp_CreateDate="
                                )
                        )
                        .map(p ->
                                p.substring(
                                        "vnp_CreateDate=".length()
                                )
                        )
                        .findFirst()
                        .orElseThrow(() ->
                                error(
                                        "Thiếu thời gian tạo giao dịch gốc để đối soát."
                                )
                        );

        if (!date.matches("[0-9]{14}")) {
            throw error(
                    "Thời gian giao dịch gốc không hợp lệ."
            );
        }

        payment.setLastQueriedAt(now);

        payments.saveAndFlush(payment);

        return new QueryTarget(
                reference,
                date
        );
    }

    @Transactional
    public String acceptQuery(
            String reference,
            String email,
            Map<String, String> fields
    ) {

        VnpayPayment payment =
                payments.findLockedByReference(reference)
                        .orElseThrow(() ->
                                new AppException(
                                        ErrorCode.RESOURCE_NOT_FOUND
                                )
                        );

        requireTenant(
                payment.getContract(),
                email
        );

        if (!"PENDING".equals(payment.getStatus())) {
            return "Giao dịch đã có kết quả xác nhận.";
        }

        // Error responses may be unsigned.
        // They are informational only and can never settle a payment.
        if (!"00".equals(
                fields.get("vnp_ResponseCode")
        )) {

            return switch (
                    Objects.toString(
                            fields.get("vnp_ResponseCode"),
                            ""
                    )
                    ) {

                case "91" ->
                        "VNPAY chưa tìm thấy giao dịch. Kiểm tra đúng merchant hoặc thử lại sau 5 phút.";

                case "94" ->
                        "VNPAY giới hạn truy vấn lặp; vui lòng kiểm tra lại sau 5 phút.";

                case "02", "97" ->
                        "VNPAY từ chối thông tin merchant hoặc chữ ký truy vấn. Cần kiểm tra cấu hình backend.";

                default ->
                        "VNPAY chưa trả kết quả đối soát hợp lệ. Vui lòng kiểm tra lại sau 5 phút.";
            };
        }

        if (!VnpaySigner.validQueryResponse(
                fields,
                config.getHashSecret()
        )
                || !Objects.equals(
                config.getTmnCode(),
                fields.get("vnp_TmnCode")
        )
                || !reference.equals(
                fields.get("vnp_TxnRef")
        )
                || !"querydr".equals(
                fields.get("vnp_Command")
        )
                || !"01".equals(
                fields.get("vnp_TransactionType")
        )
                || !Long.toString(
                payment.getAmount() * 100
        ).equals(
                fields.get("vnp_Amount")
        )) {

            throw error(
                    "Kết quả đối soát VNPAY không khớp hoặc chữ ký không hợp lệ. Chưa ghi nhận thanh toán."
            );
        }

        String status =
                fields.get(
                        "vnp_TransactionStatus"
                );

        // Only final success/failure settles.
        // Pending, refunds and unknown statuses never mark
        // an unpaid deposit paid.
        if (!"00".equals(status)
                && !"02".equals(status)) {

            return "VNPAY chưa có kết quả thanh toán cuối cùng; vui lòng kiểm tra lại sau 5 phút.";
        }

        Map<String, String> result =
                confirm(
                        payment,
                        fields
                );

        if (!"00".equals(
                result.get("RspCode")
        )) {

            throw error(
                    "Kết quả VNPAY chưa đủ thông tin để xác nhận."
            );
        }

        return "Đã đối soát trực tiếp với VNPAY.";
    }

    @Transactional
    public PaymentView create(
            UUID contractId,
            String email,
            String ip
    ) {

        if (config.getHashSecret() == null
                || config.getHashSecret().isBlank()
                || config.getHashSecret().contains("<")) {

            throw error(
                    "Chưa cấu hình VNPAY_HASH_SECRET hợp lệ trên backend."
            );
        }

        Contract contract =
                contracts.findByIdForUpdate(contractId)
                        .orElseThrow(() ->
                                new AppException(
                                        ErrorCode.CONTRACT_NOT_FOUND
                                )
                        );

        requireTenant(
                contract,
                email
        );

        Instant now =
                Instant.now();

        if (contract.getStatus()
                != ContractStatus.AWAITING_DEPOSIT
                || contract.getDepositPaidAt() != null
                || contract.getDepositDeadline() == null
                || !contract.getDepositDeadline()
                .isAfter(now)) {

            throw error(
                    "Yêu cầu chưa được mở cọc hoặc đã hết hạn."
            );
        }

        Room room =
                rooms.findByIdForUpdate(
                                contract.getRoom().getId()
                        )
                        .orElseThrow();

        if (room.getStatus()
                != RoomStatus.RESERVED
                || contract.getEndDate()
                .isBefore(
                        LocalDate.now(ZONE)
                )) {

            throw error(
                    "Phòng hoặc thời hạn thuê không còn phù hợp để thanh toán."
            );
        }

        VnpayPayment pending =
                payments
                        .findFirstByContractIdAndStatusOrderByCreatedAtDesc(
                                contractId,
                                "PENDING"
                        )
                        .orElse(null);

        if (pending != null
                && pending.getExpiresAt()
                .isAfter(now)) {

            // Repair local IPv6 links already persisted
            // before loopback normalization.
            String url =
                    pending.getPaymentUrl();

            if (url != null
                    && (
                    url.contains(
                            "vnp_IpAddr=0%3A0%3A0%3A0%3A0%3A0%3A0%3A1&"
                    )
                            || url.contains(
                            "vnp_IpAddr=%3A%3A1&"
                    )
            )) {

                Map<String, String> fields =
                        new TreeMap<>();

                for (String pair :
                        java.net.URI
                                .create(url)
                                .getRawQuery()
                                .split("&")) {

                    String[] parts =
                            pair.split(
                                    "=",
                                    2
                            );

                    fields.put(
                            parts[0],
                            java.net.URLDecoder.decode(
                                    parts[1],
                                    java.nio.charset.StandardCharsets.UTF_8
                            )
                    );
                }

                fields.put(
                        "vnp_IpAddr",
                        "127.0.0.1"
                );

                pending.setPaymentUrl(
                        config.getPayUrl()
                                + "?"
                                + VnpaySigner.query(
                                fields
                        )
                                + "&vnp_SecureHash="
                                + VnpaySigner.sign(
                                fields,
                                config.getHashSecret()
                        )
                );

                payments.save(
                        pending
                );
            }

            return view(
                    pending
            );
        }

        long amount;

        try {
            amount =
                    BigDecimal
                            .valueOf(
                                    contract.getDepositAmount()
                            )
                            .longValueExact();

        } catch (RuntimeException e) {

            throw error(
                    "Tiền cọc phải là số nguyên VNĐ hợp lệ."
            );
        }

        if (amount < 5000
                || amount > 9999999999L) {

            throw error(
                    "VNPAY yêu cầu tiền cọc từ 5.000 đến 9.999.999.999 VNĐ."
            );
        }

        VnpayPayment payment =
                new VnpayPayment();

        payment.setReference(
                UUID.randomUUID()
                        .toString()
                        .replace(
                                "-",
                                ""
                        )
        );

        payment.setContract(
                contract
        );

        payment.setAmount(
                amount
        );

        payment.setExpiresAt(
                now.plusSeconds(900)
                        .isBefore(
                                contract.getDepositDeadline()
                        )
                        ? now.plusSeconds(900)
                        : contract.getDepositDeadline()
        );

        Map<String, String> fields =
                new TreeMap<>();

        fields.put(
                "vnp_Version",
                "2.1.0"
        );

        fields.put(
                "vnp_Command",
                "pay"
        );

        fields.put(
                "vnp_TmnCode",
                config.getTmnCode()
        );

        fields.put(
                "vnp_Amount",
                Long.toString(
                        amount * 100
                )
        );

        fields.put(
                "vnp_CurrCode",
                "VND"
        );

        fields.put(
                "vnp_TxnRef",
                payment.getReference()
        );

        fields.put(
                "vnp_OrderInfo",
                "Thanh toan coc "
                        + payment.getReference()
        );

        fields.put(
                "vnp_OrderType",
                "other"
        );

        fields.put(
                "vnp_Locale",
                "vn"
        );

        fields.put(
                "vnp_ReturnUrl",
                config.getReturnUrl()
        );

        fields.put(
                "vnp_IpAddr",
                "::1".equals(ip)
                        || "0:0:0:0:0:0:0:1"
                        .equals(ip)
                        ? "127.0.0.1"
                        : ip
        );

        fields.put(
                "vnp_CreateDate",
                DATE.format(now)
        );

        fields.put(
                "vnp_ExpireDate",
                DATE.format(
                        payment.getExpiresAt()
                )
        );

        // ==============================
        // THÊM: LOG KIỂM TRA VNPAY CREATE
        // ==============================
        log.info(
                "VNPAY CREATE: tmnCode={}, txnRef={}, amount={}, createDate={}, expireDate={}, returnUrl={}, payUrl={}, ipAddr={}",
                fields.get("vnp_TmnCode"),
                fields.get("vnp_TxnRef"),
                fields.get("vnp_Amount"),
                fields.get("vnp_CreateDate"),
                fields.get("vnp_ExpireDate"),
                fields.get("vnp_ReturnUrl"),
                config.getPayUrl(),
                fields.get("vnp_IpAddr")
        );

        payment.setPaymentUrl(
                config.getPayUrl()
                        + "?"
                        + VnpaySigner.query(
                        fields
                )
                        + "&vnp_SecureHash="
                        + VnpaySigner.sign(
                        fields,
                        config.getHashSecret()
                )
        );

        return view(
                payments.save(
                        payment
                )
        );
    }

    @Transactional(readOnly = true)
    public PaymentView status(
            String reference,
            String email
    ) {

        VnpayPayment payment =
                payments.findById(reference)
                        .orElseThrow(() ->
                                new AppException(
                                        ErrorCode.RESOURCE_NOT_FOUND
                                )
                        );

        requireTenant(
                payment.getContract(),
                email
        );

        return view(
                payment
        );
    }

    @Transactional
    public Map<String, String> ipn(
            Map<String, String> fields
    ) {

        if (!VnpaySigner.valid(
                fields,
                config.getHashSecret()
        )) {

            return reply(
                    "97",
                    "Invalid signature"
            );
        }

        if (!Objects.equals(
                config.getTmnCode(),
                fields.get("vnp_TmnCode")
        )) {

            return reply(
                    "97",
                    "Invalid merchant"
            );
        }

        String reference =
                fields.get(
                        "vnp_TxnRef"
                );

        if (reference == null) {

            return reply(
                    "01",
                    "Order not found"
            );
        }

        VnpayPayment payment =
                payments
                        .findLockedByReference(reference)
                        .orElse(null);

        if (payment == null) {

            return reply(
                    "01",
                    "Order not found"
            );
        }

        if (!Long.toString(
                payment.getAmount() * 100
        ).equals(
                fields.get("vnp_Amount")
        )) {

            return reply(
                    "04",
                    "Invalid amount"
            );
        }

        if (!payment.getStatus()
                .equals("PENDING")) {

            return reply(
                    "02",
                    "Order already confirmed"
            );
        }

        return confirm(
                payment,
                fields
        );
    }

    private Map<String, String> confirm(
            VnpayPayment payment,
            Map<String, String> fields
    ) {

        String response =
                fields.get(
                        "vnp_ResponseCode"
                );

        String transactionStatus =
                fields.get(
                        "vnp_TransactionStatus"
                );

        if (response == null
                || transactionStatus == null) {

            return reply(
                    "99",
                    "Missing result"
            );
        }

        boolean success =
                "00".equals(response)
                        && "00".equals(
                        transactionStatus
                );

        if (success
                && (
                fields.get(
                        "vnp_TransactionNo"
                ) == null
                        || fields.get(
                        "vnp_TransactionNo"
                ).isBlank()
        )) {

            return reply(
                    "99",
                    "Missing transaction number"
            );
        }

        payment.setResponseCode(
                response
        );

        payment.setTransactionNo(
                fields.get(
                        "vnp_TransactionNo"
                )
        );

        payment.setCompletedAt(
                Instant.now()
        );

        payment.setStatus(
                success
                        ? "REVIEW_REQUIRED"
                        : "FAILED"
        );

        if (success) {

            Contract contract =
                    contracts
                            .findByIdForUpdate(
                                    payment
                                            .getContract()
                                            .getId()
                            )
                            .orElseThrow();

            Room room =
                    rooms
                            .findByIdForUpdate(
                                    contract
                                            .getRoom()
                                            .getId()
                            )
                            .orElseThrow();

            // Late or duplicate successful charges
            // remain recorded for reconciliation,
            // never revive a released room.
            if (contract.getStatus()
                    == ContractStatus.AWAITING_DEPOSIT
                    && contract.getDepositPaidAt()
                    == null
                    && contract.getDepositDeadline()
                    != null
                    && contract.getDepositDeadline()
                    .isAfter(
                            Instant.now()
                    )
                    && room.getStatus()
                    == RoomStatus.RESERVED
                    && !contract.getEndDate()
                    .isBefore(
                            LocalDate.now(ZONE)
                    )
                    && BigDecimal
                    .valueOf(
                            contract.getDepositAmount()
                    )
                    .compareTo(
                            BigDecimal.valueOf(
                                    payment.getAmount()
                            )
                    ) == 0) {

                DepositFinalizer.apply(
                        contract,
                        room,
                        "VNPAY-"
                                + payment.getReference()
                );

                contracts.saveAndFlush(
                        contract
                );

                payment.setStatus(
                        "SUCCESS"
                );
            }
        }

        payments.saveAndFlush(
                payment
        );

        return reply(
                "00",
                "Confirm Success"
        );
    }

    private void requireTenant(
            Contract contract,
            String email
    ) {

        if (!contract
                .getTenant()
                .getEmail()
                .equals(email)) {

            throw new AppException(
                    ErrorCode.UNAUTHORIZED
            );
        }
    }

    private PaymentView view(
            VnpayPayment p
    ) {

        return new PaymentView(
                p.getReference(),
                p.getContract().getId(),
                p.getStatus(),
                p.getAmount(),
                p.getPaymentUrl(),
                p.getExpiresAt(),
                p.getLastQueriedAt()
                        == null
                        ? null
                        : p.getLastQueriedAt()
                        .plusSeconds(300)
        );
    }

    private AppException error(
            String message
    ) {

        return new AppException(
                ErrorCode.OPERATION_NOT_ALLOWED,
                message
        );
    }

    public static Map<String, String> reply(
            String code,
            String message
    ) {

        return Map.of(
                "RspCode",
                code,
                "Message",
                message
        );
    }
}