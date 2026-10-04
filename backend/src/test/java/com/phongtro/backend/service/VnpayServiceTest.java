package com.phongtro.backend.service;

import com.phongtro.backend.config.VnpayProperties;
import com.phongtro.backend.entity.*;
import com.phongtro.backend.exception.AppException;
import com.phongtro.backend.repository.*;
import org.junit.jupiter.api.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VnpayServiceTest {
    VnpayProperties config;
    VnpayPaymentRepository payments;
    ContractRepository contracts;
    RoomRepository rooms;
    VnpayService service;
    Contract contract;
    Room room;
    VnpayPayment payment;

    @BeforeEach void setup() {
        config = new VnpayProperties();
        config.setTmnCode("3IINFGVH"); config.setHashSecret("test-secret");
        config.setPayUrl("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
        config.setReturnUrl("http://localhost:5173/payment/vnpay-return");
        payments = mock(VnpayPaymentRepository.class); contracts = mock(ContractRepository.class); rooms = mock(RoomRepository.class);
        service = new VnpayService(config, payments, contracts, rooms);
        User tenant = User.builder().id(UUID.randomUUID()).email("tenant@test.vn").fullName("Tenant").build();
        User landlord = User.builder().id(UUID.randomUUID()).email("owner@test.vn").fullName("Owner").build();
        room = Room.builder().id(UUID.randomUUID()).status(RoomStatus.RESERVED).title("Room").address("Address").build();
        contract = Contract.builder().id(UUID.randomUUID()).tenant(tenant).landlord(landlord).room(room)
                .status(ContractStatus.AWAITING_DEPOSIT).depositAmount(1000000d).monthlyRent(2000000d)
                .depositDeadline(Instant.now().plusSeconds(3600)).startDate(LocalDate.now()).endDate(LocalDate.now().plusMonths(6)).build();
        payment = new VnpayPayment(); payment.setReference("abc123"); payment.setContract(contract);
        payment.setAmount(1000000); payment.setExpiresAt(Instant.now().plusSeconds(900));
        when(payments.findLockedByReference("abc123")).thenReturn(Optional.of(payment));
        when(contracts.findByIdForUpdate(contract.getId())).thenReturn(Optional.of(contract));
        when(rooms.findByIdForUpdate(room.getId())).thenReturn(Optional.of(room));
        when(payments.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    Map<String, String> callback(String code) {
        Map<String, String> fields = new HashMap<>(Map.of("vnp_TmnCode", "3IINFGVH", "vnp_TxnRef", "abc123",
                "vnp_Amount", "100000000", "vnp_ResponseCode", code, "vnp_TransactionStatus", code, "vnp_TransactionNo", "123456"));
        return sign(fields);
    }
    Map<String, String> sign(Map<String, String> fields) {
        fields.put("vnp_SecureHash", VnpaySigner.sign(fields, config.getHashSecret())); return fields;
    }

    @Test void successFinalizesContractAndDuplicateDoesNotWriteAgain() {
        var fields = callback("00");
        assertEquals("00", service.ipn(fields).get("RspCode"));
        assertEquals("SUCCESS", payment.getStatus());
        assertEquals(ContractStatus.AWAITING_SIGNATURES, contract.getStatus());
        assertEquals("VNPAY-abc123", contract.getPaymentReference());
        assertNotNull(contract.getDocumentContent());
        assertEquals("02", service.ipn(fields).get("RspCode"));
        verify(contracts, times(1)).saveAndFlush(contract);
    }
    @Test void tamperedSignatureCannotChangeState() {
        var fields = callback("00"); fields.put("vnp_Amount", "1");
        assertEquals("97", service.ipn(fields).get("RspCode"));
        verify(payments, never()).findLockedByReference(any());
        assertEquals("PENDING", payment.getStatus());
    }
    @Test void signedWrongAmountRejected() {
        var fields = callback("00"); fields.put("vnp_Amount", "100");
        assertEquals("04", service.ipn(sign(fields)).get("RspCode"));
        verify(contracts, never()).saveAndFlush(any());
    }
    @Test void wrongMerchantRejected() {
        var fields = callback("00"); fields.put("vnp_TmnCode", "WRONG");
        assertEquals("97", service.ipn(sign(fields)).get("RspCode"));
    }
    @Test void unknownReferenceRejected() {
        var fields = callback("00"); fields.put("vnp_TxnRef", "unknown");
        assertEquals("01", service.ipn(sign(fields)).get("RspCode"));
    }
    @Test void cancellationDoesNotMarkDepositPaid() {
        assertEquals("00", service.ipn(callback("24")).get("RspCode"));
        assertEquals("FAILED", payment.getStatus()); assertNull(contract.getDepositPaidAt());
    }
    @Test void transactionStatusMustAlsoBeSuccessful() {
        var fields = callback("00"); fields.put("vnp_TransactionStatus", "02");
        service.ipn(sign(fields));
        assertEquals("FAILED", payment.getStatus()); assertNull(contract.getDepositPaidAt());
    }
    @Test void lateSuccessIsRecordedForReviewWithoutRevivingContract() {
        contract.setStatus(ContractStatus.CANCELLED);
        assertEquals("00", service.ipn(callback("00")).get("RspCode"));
        assertEquals("REVIEW_REQUIRED", payment.getStatus());
        assertEquals(ContractStatus.CANCELLED, contract.getStatus()); assertNull(contract.getDepositPaidAt());
    }
    @Test void expiredDepositNeedsReview() {
        contract.setDepositDeadline(Instant.now().minusSeconds(10));
        service.ipn(callback("00")); assertEquals("REVIEW_REQUIRED", payment.getStatus());
    }
    @Test void createUsesServerAmountAndSignedUrl() {
        var result = service.create(contract.getId(), "tenant@test.vn", "127.0.0.1");
        assertEquals(1000000, result.amount());
        assertTrue(result.paymentUrl().contains("vnp_Amount=100000000"));
        Map<String, String> fields = new HashMap<>();
        for (String pair : java.net.URI.create(result.paymentUrl()).getRawQuery().split("&")) {
            String[] parts = pair.split("=", 2);
            fields.put(parts[0], java.net.URLDecoder.decode(parts[1], java.nio.charset.StandardCharsets.UTF_8));
        }
        assertTrue(VnpaySigner.valid(fields, config.getHashSecret()));
        assertFalse(fields.containsKey("vnp_IpnUrl"));
    }
    @Test void repeatedCreateReusesPendingPayment() {
        when(payments.findFirstByContractIdAndStatusOrderByCreatedAtDesc(contract.getId(), "PENDING")).thenReturn(Optional.of(payment));
        assertEquals("abc123", service.create(contract.getId(), "tenant@test.vn", "127.0.0.1").reference());
        verify(payments, never()).save(any());
    }
    @Test void otherUserCannotPayOrRead() {
        assertThrows(AppException.class, () -> service.create(contract.getId(), "other@test.vn", "127.0.0.1"));
        when(payments.findById("abc123")).thenReturn(Optional.of(payment));
        assertThrows(AppException.class, () -> service.status("abc123", "other@test.vn"));
    }
    @Test void missingSecretFailsBeforeCreatingPayment() {
        config.setHashSecret("");
        assertThrows(AppException.class, () -> service.create(contract.getId(), "tenant@test.vn", "127.0.0.1"));
        verify(payments, never()).save(any());
    }
    @Test void zeroOrFractionalAmountRejected() {
        contract.setDepositAmount(0d);
        assertThrows(AppException.class, () -> service.create(contract.getId(), "tenant@test.vn", "127.0.0.1"));
        contract.setDepositAmount(5000.5);
        assertThrows(AppException.class, () -> service.create(contract.getId(), "tenant@test.vn", "127.0.0.1"));
    }

    @Test void signatureMatchesIndependentHmacFixture() {
        var fields = Map.of("vnp_TmnCode", "3IINFGVH", "vnp_OrderInfo", "Thanh toan coc", "vnp_Amount", "100000000");
        assertEquals("09fc281ce8cd78e2ca86a55e4fcb47533a0a8921f5b949837195da2410401ffe9ef11c9146d6d02ea3fe24c3b5c26a29cfab0980fcb2ff4f52ece4a246b36df0",
                VnpaySigner.sign(fields, "test-secret"));
    }

    @Test void localIpv6UsesIpv4Loopback() {
        for (String ip : List.of("::1", "0:0:0:0:0:0:0:1")) {
            var result = service.create(contract.getId(), "tenant@test.vn", ip);
            assertTrue(result.paymentUrl().contains("vnp_IpAddr=127.0.0.1&"));
        }
    }

    @Test void repairsExistingIpv6LinkWithoutCreatingAnotherCharge() {
        payment.setPaymentUrl(config.getPayUrl() + "?vnp_IpAddr=0%3A0%3A0%3A0%3A0%3A0%3A0%3A1&vnp_TxnRef=abc123&vnp_SecureHash=old");
        when(payments.findFirstByContractIdAndStatusOrderByCreatedAtDesc(contract.getId(), "PENDING")).thenReturn(Optional.of(payment));
        var result = service.create(contract.getId(), "tenant@test.vn", "::1");
        assertEquals("abc123", result.reference());
        assertTrue(result.paymentUrl().contains("vnp_IpAddr=127.0.0.1&"));
        assertFalse(result.paymentUrl().contains("vnp_SecureHash=old"));
        Map<String, String> fields = Map.of("vnp_IpAddr", "127.0.0.1", "vnp_TxnRef", "abc123");
        assertTrue(result.paymentUrl().endsWith("vnp_SecureHash=" + VnpaySigner.sign(fields, config.getHashSecret())));
        verify(payments).save(payment);
    }

    Map<String, String> queryResponse(String status) {
        Map<String, String> fields = new HashMap<>(Map.of("vnp_TmnCode", "3IINFGVH", "vnp_TxnRef", "abc123",
                "vnp_Amount", "100000000", "vnp_ResponseCode", "00", "vnp_TransactionStatus", status,
                "vnp_TransactionNo", "123456", "vnp_Command", "querydr", "vnp_TransactionType", "01"));
        fields.put("vnp_ResponseId", "response123"); fields.put("vnp_Message", "Success");
        return signQuery(fields);
    }
    Map<String, String> signQuery(Map<String, String> fields) {
        fields.put("vnp_SecureHash", VnpaySigner.hmac(VnpaySigner.joined(fields, "vnp_ResponseId", "vnp_Command",
                "vnp_ResponseCode", "vnp_Message", "vnp_TmnCode", "vnp_TxnRef", "vnp_Amount", "vnp_BankCode",
                "vnp_PayDate", "vnp_TransactionNo", "vnp_TransactionType", "vnp_TransactionStatus", "vnp_OrderInfo",
                "vnp_PromotionCode", "vnp_PromotionAmount"), config.getHashSecret()));
        return fields;
    }
    @Test void queryAndIpnShareIdempotentSettlement() {
        service.acceptQuery("abc123", "tenant@test.vn", queryResponse("00"));
        assertEquals("SUCCESS", payment.getStatus());
        assertEquals(ContractStatus.AWAITING_SIGNATURES, contract.getStatus());
        assertEquals("02", service.ipn(callback("00")).get("RspCode"));
        verify(contracts, times(1)).saveAndFlush(contract);
    }
    @Test void invalidQuerySignatureCannotConfirmDeposit() {
        var response = queryResponse("00"); response.put("vnp_Amount", "100");
        assertThrows(AppException.class, () -> service.acceptQuery("abc123", "tenant@test.vn", response));
        assertNull(contract.getDepositPaidAt());
    }
    @Test void signedWrongReferenceOrAmountOrMerchantIsRejected() {
        for (String key : List.of("vnp_TxnRef", "vnp_Amount", "vnp_TmnCode", "vnp_TransactionType")) {
            var response = queryResponse("00"); response.put(key, "wrong"); signQuery(response);
            assertThrows(AppException.class, () -> service.acceptQuery("abc123", "tenant@test.vn", response));
        }
        verify(contracts, never()).saveAndFlush(any());
    }
    @Test void queryApiSuccessDoesNotMeanPaymentSuccess() {
        service.acceptQuery("abc123", "tenant@test.vn", queryResponse("01"));
        assertEquals("PENDING", payment.getStatus());
        service.acceptQuery("abc123", "tenant@test.vn", queryResponse("02"));
        assertEquals("FAILED", payment.getStatus()); assertNull(contract.getDepositPaidAt());
    }
    @Test void queryNotFoundDoesNotMarkPaymentFailed() {
        assertTrue(service.acceptQuery("abc123", "tenant@test.vn", Map.of("vnp_ResponseCode", "91")).contains("chưa tìm thấy"));
        assertEquals("PENDING", payment.getStatus());
    }
    @Test void queryReservationUsesExactStoredDateAndEnforcesFiveMinutes() {
        payment.setPaymentUrl(config.getPayUrl()+"?vnp_CreateDate=20260928223527&vnp_TxnRef=abc123");
        assertEquals("20260928223527", service.reserveQuery("abc123", "tenant@test.vn").transactionDate());
        assertNull(service.reserveQuery("abc123", "tenant@test.vn"));
        payment.setLastQueriedAt(Instant.now().minusSeconds(301));
        assertNotNull(service.reserveQuery("abc123", "tenant@test.vn"));
    }
    @Test void queryRequiresPaymentOwner() {
        assertThrows(AppException.class, () -> service.reserveQuery("abc123", "other@test.vn"));
        assertThrows(AppException.class, () -> service.acceptQuery("abc123", "other@test.vn", queryResponse("00")));
        verify(payments, never()).saveAndFlush(any());
    }
}
