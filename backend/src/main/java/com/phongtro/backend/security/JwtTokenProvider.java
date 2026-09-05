package com.phongtro.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    private SecretKey key;

    @PostConstruct
    public void init() {
        byte[] keyBytes;
        try {
            // Thử giải mã Base64 trước
            keyBytes = Decoders.BASE64.decode(jwtSecret);
        } catch (IllegalArgumentException e) {
            // Nếu không phải chuỗi Base64 chuẩn, dùng byte UTF-8
            keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Tạo JWT Access Token chứa email, userId và role
     */
    public String generateAccessToken(UUID userId, String email, String role) {
        Instant now = Instant.now();
        Instant expiryDate = now.plusMillis(jwtExpirationMs);

        return Jwts.builder()
                .subject(email)
                .claim("userId", userId.toString())
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiryDate))
                .signWith(key)
                .compact();
    }

    /**
     * Trích xuất email (subject) từ JWT token
     */
    public String getEmailFromToken(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * Trích xuất userId từ JWT token
     */
    public UUID getUserIdFromToken(String token) {
        String userIdStr = getClaims(token).get("userId", String.class);
        return UUID.fromString(userIdStr);
    }

    /**
     * Trích xuất Role từ JWT token
     */
    public String getRoleFromToken(String token) {
        return getClaims(token).get("role", String.class);
    }

    /**
     * Kiểm tra tính hợp lệ của token
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT token đã hết hạn: {}", e.getMessage());
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT token không hợp lệ: {}", e.getMessage());
        }
        return false;
    }

    public long getExpirationMs() {
        return jwtExpirationMs;
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
