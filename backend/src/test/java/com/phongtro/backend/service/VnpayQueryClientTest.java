package com.phongtro.backend.service;

import com.phongtro.backend.config.VnpayProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class VnpayQueryClientTest {
    @Test void sendsOfficialQueryShapeWithOriginalDateAndPipeDelimitedHmac() {
        var config = new VnpayProperties(); config.setTmnCode("3IINFGVH"); config.setHashSecret("test-secret");
        var builder = RestClient.builder(); var server = MockRestServiceServer.bindTo(builder).build();
        var client = new VnpayQueryClient(config, builder.build());
        server.expect(requestTo(config.getQueryUrl())).andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(request -> {
                    var fields = new ObjectMapper().readValue(((MockClientHttpRequest) request).getBodyAsString(),
                            new TypeReference<Map<String, String>>() {});
                    assertEquals("querydr", fields.get("vnp_Command"));
                    assertEquals("20260928223527", fields.get("vnp_TransactionDate"));
                    assertEquals("abc123", fields.get("vnp_TxnRef"));
                    assertFalse(fields.containsKey("vnp_HashSecret"));
                    String data = fields.get("vnp_RequestId")+"|2.1.0|querydr|3IINFGVH|abc123|20260928223527|"
                            +fields.get("vnp_CreateDate")+"|127.0.0.1|Kiem tra thanh toan coc abc123";
                    assertEquals(VnpaySigner.hmac(data,"test-secret"),fields.get("vnp_SecureHash"));
                }).andRespond(withSuccess("{\"vnp_ResponseCode\":\"91\"}",MediaType.APPLICATION_JSON));
        assertEquals("91",client.query(new VnpayService.QueryTarget("abc123","20260928223527")).get("vnp_ResponseCode"));
        server.verify();
    }
}
