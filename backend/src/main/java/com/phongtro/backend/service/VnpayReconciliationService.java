package com.phongtro.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

@Service
@RequiredArgsConstructor
public class VnpayReconciliationService {
    private final VnpayService payments;
    private final VnpayQueryClient client;
    public record Result(VnpayService.PaymentView payment, String message) {}

    // The HTTP request runs outside the short DB transactions so IPN is never blocked by network I/O.
    public Result reconcile(String reference, String email) {
        var target = payments.reserveQuery(reference, email);
        if (target == null) return new Result(payments.status(reference, email),
                "Đã kiểm tra gần đây hoặc giao dịch đã có kết quả. VNPAY giới hạn truy vấn lặp trong 5 phút.");
        String message;
        try {
            var response = client.query(target);
            message = payments.acceptQuery(reference, email, response);
        } catch (RestClientException e) {
            message = "Chưa kết nối được VNPAY để đối soát. Chưa xác nhận thanh toán; vui lòng kiểm tra lại sau 5 phút.";
        }
        return new Result(payments.status(reference, email), message);
    }
}
