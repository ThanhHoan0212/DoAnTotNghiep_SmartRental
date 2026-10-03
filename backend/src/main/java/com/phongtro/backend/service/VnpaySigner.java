package com.phongtro.backend.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class VnpaySigner {
    private VnpaySigner() {}

    public static String query(Map<String, String> fields) {
        return new TreeMap<>(fields).entrySet().stream()
                .filter(e -> e.getKey().startsWith("vnp_") && !e.getKey().equals("vnp_SecureHash")
                        && !e.getKey().equals("vnp_SecureHashType") && e.getValue() != null && !e.getValue().isEmpty())
                .map(e -> encode(e.getKey()) + "=" + encode(e.getValue())).collect(Collectors.joining("&"));
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public static String sign(Map<String, String> fields, String secret) {
        return hmac(query(fields), secret);
    }

    public static String hmac(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException("Cannot sign VNPAY request", e);
        }
    }

    public static boolean valid(Map<String, String> fields, String secret) {
        String hash = fields.get("vnp_SecureHash");
        return secret != null && !secret.isBlank() && hash != null && hash.matches("[a-fA-F0-9]{128}")
                && MessageDigest.isEqual(sign(fields, secret).getBytes(StandardCharsets.US_ASCII),
                hash.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII));
    }

    public static String joined(Map<String, String> fields, String... keys) {
        return Arrays.stream(keys).map(k -> Objects.toString(fields.get(k), "")).collect(Collectors.joining("|"));
    }

    public static boolean validQueryResponse(Map<String, String> fields, String secret) {
        String hash = fields.get("vnp_SecureHash");
        if (secret == null || secret.isBlank() || hash == null || !hash.matches("[a-fA-F0-9]{128}")) return false;
        String data = joined(fields, "vnp_ResponseId", "vnp_Command", "vnp_ResponseCode", "vnp_Message",
                "vnp_TmnCode", "vnp_TxnRef", "vnp_Amount", "vnp_BankCode", "vnp_PayDate", "vnp_TransactionNo",
                "vnp_TransactionType", "vnp_TransactionStatus", "vnp_OrderInfo", "vnp_PromotionCode", "vnp_PromotionAmount");
        return MessageDigest.isEqual(HexFormat.of().parseHex(hash), HexFormat.of().parseHex(hmac(data, secret)));
    }
}
