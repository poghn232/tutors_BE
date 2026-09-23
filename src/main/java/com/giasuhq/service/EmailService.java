package com.giasuhq.service;

import java.util.concurrent.CompletableFuture;

public interface EmailService {

    boolean sendOtpEmail(String toEmail, String otpCode);

    boolean sendRegisterOtpEmail(String toEmail, String otpCode);

    CompletableFuture<Boolean> sendOtpEmailAsync(String toEmail, String otpCode);

    CompletableFuture<Boolean> sendRegisterOtpEmailAsync(String toEmail, String otpCode);

    boolean isMailConfigured();
}
