package com.giasuhq.service.impl;

import com.giasuhq.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Override
    @Async
    public void sendOtpEmail(String toEmail, String otpCode) {
        log.info("Preparing OTP email for recipient: {}", toEmail);

        if (mailSender == null || fromEmail == null || fromEmail.isBlank() || mailPassword == null || mailPassword.isBlank()) {
            log.warn("=================================================");
            log.warn("JavaMailSender or MAIL_PASSWORD is not fully configured.");
            log.warn("FORGOT PASSWORD OTP for {}: [{}]", toEmail, otpCode);
            log.warn("=================================================");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "Gia Sư HQ Support");
            helper.setTo(toEmail);
            helper.setSubject("[Gia Sư HQ] Mã xác nhận đặt lại mật khẩu: " + otpCode);

            String htmlBody = buildOtpHtmlContent(toEmail, otpCode);
            helper.setText(htmlBody, true);

            mailSender.send(message);
            log.info("OTP email successfully sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}. [DEV OTP CODE: {}]", toEmail, e.getMessage(), otpCode);
        }
    }

    private String buildOtpHtmlContent(String toEmail, String otpCode) {
        return "<!DOCTYPE html>"
                + "<html>"
                + "<head><meta charset='UTF-8'></head>"
                + "<body style='font-family: -apple-system, BlinkMacSystemFont, Segoe UI, Roboto, Helvetica, Arial, sans-serif; background-color: #f8fafc; margin: 0; padding: 30px; color: #1e293b;'>"
                + "  <div style='max-width: 520px; margin: 0 auto; background: #ffffff; border-radius: 16px; border: 1.5px solid #e2e8f0; overflow: hidden; box-shadow: 0 4px 14px rgba(0,0,0,0.05);'>"
                + "    <div style='background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%); padding: 24px; text-align: center; color: #ffffff;'>"
                + "      <h1 style='margin: 0; font-size: 22px; font-weight: 800; letter-spacing: 0.5px;'>Gia Sư HQ · Tutora</h1>"
                + "      <p style='margin: 6px 0 0 0; font-size: 13px; opacity: 0.9;'>Nền tảng kết nối gia sư và học sinh hàng đầu</p>"
                + "    </div>"
                + "    <div style='padding: 28px 24px;'>"
                + "      <h2 style='font-size: 18px; font-weight: 700; color: #0f172a; margin-top: 0;'>Yêu cầu đặt lại mật khẩu</h2>"
                + "      <p style='font-size: 14px; line-height: 1.6; color: #475569; margin-bottom: 20px;'>"
                + "        Xin chào,<br/>"
                + "        Hệ thống nhận được yêu cầu đặt lại mật khẩu cho tài khoản <strong>" + toEmail + "</strong>. "
                + "        Dưới đây là mã xác thực OTP của bạn:"
                + "      </p>"
                + "      <div style='background: #eff6ff; border: 2px dashed #3b82f6; border-radius: 12px; padding: 20px; text-align: center; margin: 24px 0;'>"
                + "        <div style='font-size: 12px; font-weight: 700; text-transform: uppercase; color: #64748b; margin-bottom: 6px; letter-spacing: 1px;'>Mã xác nhận (OTP)</div>"
                + "        <span style='font-family: Consolas, Monaco, monospace; font-size: 36px; font-weight: 900; letter-spacing: 8px; color: #1d4ed8;'>" + otpCode + "</span>"
                + "        <div style='font-size: 12px; color: #94a3b8; margin-top: 6px;'>Hiệu lực trong 10 phút</div>"
                + "      </div>"
                + "      <div style='background: #fffbeb; border-left: 4px solid #f59e0b; padding: 12px 14px; border-radius: 6px; font-size: 13px; color: #92400e; margin-bottom: 24px;'>"
                + "        <strong>Lưu ý bảo mật:</strong> Không chia sẻ mã này cho bất kỳ ai. Nhân viên Gia Sư HQ không bao giờ yêu cầu bạn cung cấp mã OTP."
                + "      </div>"
                + "      <p style='font-size: 13px; line-height: 1.5; color: #64748b; margin: 0;'>"
                + "        Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email hoặc đổi mật khẩu để bảo vệ tài khoản.<br/><br/>"
                + "        Trân trọng,<br/>"
                + "        <strong>Ban quản trị Gia Sư HQ</strong>"
                + "      </p>"
                + "    </div>"
                + "    <div style='background: #f8fafc; padding: 14px; text-align: center; font-size: 11px; color: #94a3b8; border-top: 1px solid #f1f5f9;'>"
                + "      © 2026 Gia Sư HQ. Mọi quyền được bảo lưu."
                + "    </div>"
                + "  </div>"
                + "</body>"
                + "</html>";
    }
}
