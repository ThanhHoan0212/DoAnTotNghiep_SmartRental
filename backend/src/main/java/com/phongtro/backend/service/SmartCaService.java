package com.phongtro.backend.service;

import com.phongtro.backend.dto.smartca.*;

import java.util.List;
import java.util.UUID;

public interface SmartCaService {

    SmartCaInitiateResponse initiateSigning(UUID contractId, String userEmail, SmartCaInitiateRequest request);

    SmartCaStatusResponse confirmSigning(UUID contractId, String userEmail, SmartCaConfirmRequest request);

    SmartCaStatusResponse getSigningStatus(UUID contractId, String userEmail, String transactionId);

    List<SmartCaCertificateInfo> getCertificates(String userEmail);
}
