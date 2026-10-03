package com.phongtro.backend.service;

import com.phongtro.backend.config.VnpayProperties;
import com.phongtro.backend.exception.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.core.ParameterizedTypeReference;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class VnpayQueryClient {
    private final VnpayProperties config;
    private final RestClient http;

    @org.springframework.beans.factory.annotation.Autowired
    public VnpayQueryClient(VnpayProperties config) {
        this.config = config;
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        this.http = RestClient.builder().requestFactory(factory).build();
    }

    VnpayQueryClient(VnpayProperties config, RestClient http) {
        this.config = config;
        this.http = http;
    }

    public Map<String, String> query(VnpayService.QueryTarget target) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("vnp_RequestId", UUID.randomUUID().toString().replace("-", ""));
        fields.put("vnp_Version", "2.1.0"); fields.put("vnp_Command", "querydr");
        fields.put("vnp_TmnCode", config.getTmnCode()); fields.put("vnp_TxnRef", target.reference());
        fields.put("vnp_TransactionDate", target.transactionDate());
        fields.put("vnp_CreateDate", DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                .withZone(ZoneId.of("Asia/Ho_Chi_Minh")).format(Instant.now()));
        fields.put("vnp_IpAddr", config.getQueryIp());
        fields.put("vnp_OrderInfo", "Kiem tra thanh toan coc " + target.reference());
        fields.put("vnp_SecureHash", VnpaySigner.hmac(VnpaySigner.joined(fields, "vnp_RequestId", "vnp_Version",
                "vnp_Command", "vnp_TmnCode", "vnp_TxnRef", "vnp_TransactionDate", "vnp_CreateDate",
                "vnp_IpAddr", "vnp_OrderInfo"), config.getHashSecret()));
        Map<String, String> result = http.post().uri(config.getQueryUrl())
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON).body(fields)
                .retrieve().body(new ParameterizedTypeReference<Map<String, String>>() {});
        if (result == null) throw new AppException(ErrorCode.OPERATION_NOT_ALLOWED, "VNPAY trả về dữ liệu trống.");
        return result;
    }
}
