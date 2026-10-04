package com.phongtro.backend.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VnpayReconciliationServiceTest {
    @Test void cooldownDoesNotCallProvider() {
        var payments = mock(VnpayService.class); var client = mock(VnpayQueryClient.class);
        new VnpayReconciliationService(payments,client).reconcile("ref","tenant");
        verifyNoInteractions(client);
    }
    @Test void networkFailureDoesNotSettlePayment() {
        var payments = mock(VnpayService.class); var client = mock(VnpayQueryClient.class);
        var target = new VnpayService.QueryTarget("ref","20260928223527");
        when(payments.reserveQuery("ref","tenant")).thenReturn(target);
        when(client.query(target)).thenThrow(new ResourceAccessException("timeout"));
        var result = new VnpayReconciliationService(payments,client).reconcile("ref","tenant");
        assertTrue(result.message().contains("Chưa kết nối"));
        verify(payments,never()).acceptQuery(any(),any(),any());
    }
}
