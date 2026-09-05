package com.phongtro.backend.service;

public interface EmailService {

    void sendPasswordResetEmail(String toEmail, String token);

    void sendWelcomeEmail(String toEmail, String fullName);
}
