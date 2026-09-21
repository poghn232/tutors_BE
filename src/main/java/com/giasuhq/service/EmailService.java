package com.giasuhq.service;

public interface EmailService {

    void sendOtpEmail(String toEmail, String otpCode);
}
