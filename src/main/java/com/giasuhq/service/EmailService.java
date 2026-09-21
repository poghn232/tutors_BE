package com.giasuhq.service;

public interface EmailService {

    void sendOtpEmail(String toEmail, String otpCode);

    void sendRegisterOtpEmail(String toEmail, String otpCode);
}
