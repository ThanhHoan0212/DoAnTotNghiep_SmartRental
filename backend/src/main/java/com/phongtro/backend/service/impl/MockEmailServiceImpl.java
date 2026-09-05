package com.phongtro.backend.service.impl;

import com.phongtro.backend.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MockEmailServiceImpl implements EmailService {

    @Override
    public void sendPasswordResetEmail(String toEmail, String token) {
        log.info("==================================================================");
        log.info("[MOCK EMAIL] Gửi email đặt lại mật khẩu tới: {}", toEmail);
        log.info("[MOCK EMAIL] Mã xác nhận/Token đặt lại mật khẩu: {}", token);
        log.info("[MOCK EMAIL] Sử dụng mã này với endpoint /api/v1/auth/reset-password");
        log.info("==================================================================");
    }

    @Override
    public void sendWelcomeEmail(String toEmail, String fullName) {
        log.info("[MOCK EMAIL] Chào mừng người dùng mới: {} ({}) đến với hệ thống Smart Rental", fullName, toEmail);
    }
}
