package com.giasuhq.service;

public interface EmailService {

    boolean sendOtpEmail(String toEmail, String otpCode);

    boolean sendRegisterOtpEmail(String toEmail, String otpCode);

    boolean isMailConfigured();
}
