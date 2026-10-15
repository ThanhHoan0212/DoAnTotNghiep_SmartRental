package com.phongtro.backend.service.client;

import com.phongtro.backend.dto.smartca.SmartCaCertificateInfo;

import java.util.List;

public interface VnptSmartCaClient {

    String getAccessToken();

    List<SmartCaCertificateInfo> getCertificates(String userId);

    SmartCaSignInitResult initiateSign(String userId, String userName, String transactionId, String docId, String dataHashHex, String serialNumber);

    SmartCaConfirmResult confirmSign(String userId, String userName, String transactionId, String pin, String sad, boolean forceSandbox);

    SmartCaStatusResult checkSignStatus(String transactionId, String userId, String userName);

    boolean registerCertificate(String uid, String fullName, String phone, String email);

    record SmartCaSignInitResult(
            boolean success,
            String transactionId,
            String status,
            String message,
            boolean isSandbox
    ) {}

    record SmartCaConfirmResult(
            boolean success,
            String transactionId,
            String status,
            String message,
            String signatureValue,
            String certSerial,
            String certSubject
    ) {}

    record SmartCaStatusResult(
            boolean success,
            String transactionId,
            String status,
            String message,
            String signatureValue,
            String certSerial,
            String certSubject,
            boolean isSigned
    ) {}
}
