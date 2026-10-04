package com.phongtro.backend.controller;

import com.phongtro.backend.common.ApiResponse;
import com.phongtro.backend.service.VnpayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequiredArgsConstructor
@Slf4j
public class VnpayController {

    private final VnpayService service;
    private final com.phongtro.backend.service.VnpayReconciliationService reconciliation;

    @PostMapping("/api/v1/payments/vnpay/{reference}/reconcile")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<com.phongtro.backend.service.VnpayReconciliationService.Result> reconcile(
            @PathVariable String reference,
            Authentication auth) {

        return ApiResponse.success(
                reconciliation.reconcile(reference, auth.getName())
        );
    }

    @PostMapping("/api/v1/payments/vnpay/contracts/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<VnpayService.PaymentView> create(
            @PathVariable UUID id,
            Authentication auth,
            HttpServletRequest request) {

        return ApiResponse.success(
                service.create(
                        id,
                        auth.getName(),
                        request.getRemoteAddr()
                )
        );
    }

    @GetMapping("/api/v1/payments/vnpay/{reference}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<VnpayService.PaymentView> status(
            @PathVariable String reference,
            Authentication auth) {

        return ApiResponse.success(
                service.status(reference, auth.getName())
        );
    }

    @GetMapping({
            "/api/payments/vnpay/ipn",
            "/api/v1/payments/vnpay/ipn"
    })
    public Map<String, String> ipn(HttpServletRequest request) {

        // ==============================
        // THÊM: LOG KIỂM TRA IPN VNPAY
        // ==============================
        log.info(
                "VNPAY IPN: tmnCode={}, txnRef={}, transactionNo={}, responseCode={}, transactionStatus={}, payDate={}, bankCode={}, amount={}",
                request.getParameter("vnp_TmnCode"),
                request.getParameter("vnp_TxnRef"),
                request.getParameter("vnp_TransactionNo"),
                request.getParameter("vnp_ResponseCode"),
                request.getParameter("vnp_TransactionStatus"),
                request.getParameter("vnp_PayDate"),
                request.getParameter("vnp_BankCode"),
                request.getParameter("vnp_Amount")
        );

        Map<String, String> fields = new HashMap<>();

        for (var entry : request.getParameterMap().entrySet()) {

            if (entry.getValue().length != 1) {
                return VnpayService.reply(
                        "97",
                        "Duplicate parameter"
                );
            }

            fields.put(
                    entry.getKey(),
                    entry.getValue()[0]
            );
        }

        try {
            return service.ipn(fields);

        } catch (RuntimeException e) {

            log.error(
                    "VNPAY IPN transaction failed; provider should retry",
                    e
            );

            return VnpayService.reply(
                    "99",
                    "Please retry"
            );
        }
    }
}