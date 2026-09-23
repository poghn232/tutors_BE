package com.giasuhq.service.impl;

import com.giasuhq.service.EmailService;
import com.giasuhq.exception.EmailDeliveryException;
import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.net.ssl.SSLHandshakeException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Set;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Value("${spring.mail.host:}")
    private String smtpHost;

    @Value("${spring.mail.port:}")
    private String smtpPort;

    @Value("${spring.mail.properties.mail.smtp.connectiontimeout:}")
    private String connectionTimeoutMs;

    @Value("${spring.mail.properties.mail.smtp.timeout:}")
    private String readTimeoutMs;

    @Value("${spring.mail.properties.mail.smtp.writetimeout:}")
    private String writeTimeoutMs;

    @Override
    public boolean isMailConfigured() {
        return mailSender != null && hasText(fromEmail) && hasText(mailPassword);
    }

    @Override
    public boolean sendOtpEmail(String toEmail, String otpCode) {
        log.info("Preparing password-reset OTP email for recipient={}", maskEmail(toEmail));
        return sendHtmlEmail(
                "PASSWORD_RESET",
                toEmail,
                "[Gia Sư HQ] Mã xác nhận đặt lại mật khẩu",
                buildOtpHtmlContent(toEmail, otpCode)
        );
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

    @Override
    public boolean sendRegisterOtpEmail(String toEmail, String otpCode) {
        log.info("Preparing registration OTP email for recipient={}", maskEmail(toEmail));
        return sendHtmlEmail(
                "REGISTRATION",
                toEmail,
                "[Gia Sư HQ] Mã xác thực kích hoạt tài khoản",
                buildRegisterOtpHtmlContent(toEmail, otpCode)
        );
    }

    private boolean sendHtmlEmail(String mailType, String toEmail, String subject, String htmlBody) {
        String recipient = maskEmail(toEmail);
        long startedAt = System.nanoTime();

        if (!isMailConfigured()) {
            log.warn(
                    "event=SMTP_NOT_CONFIGURED mailType={} recipient={} mailSenderAvailable={} usernameConfigured={} passwordConfigured={} smtpHost={} smtpPort={}. OTP delivery skipped.",
                    mailType,
                    recipient,
                    mailSender != null,
                    hasText(fromEmail),
                    hasText(mailPassword),
                    configValue(smtpHost),
                    configValue(smtpPort)
            );
            return false;
        }

        log.info(
                "event=SMTP_SEND_STARTED mailType={} recipient={} smtpHost={} smtpPort={} connectionTimeoutMs={} readTimeoutMs={} writeTimeoutMs={}",
                mailType,
                recipient,
                configValue(smtpHost),
                configValue(smtpPort),
                configValue(connectionTimeoutMs),
                configValue(readTimeoutMs),
                configValue(writeTimeoutMs)
        );

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, "Gia Sư HQ Support");
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("event=SMTP_SEND_SUCCEEDED mailType={} recipient={} elapsedMs={}", mailType, recipient, elapsedMillis(startedAt));
            return true;
        } catch (Exception exception) {
            MailFailure failure = classifyMailFailure(exception);
            Throwable rootCause = findRootCause(exception);
            log.error(
                    "event=SMTP_SEND_FAILED mailType={} category={} recipient={} smtpHost={} smtpPort={} elapsedMs={} rootCauseType={} rootCauseMessage={} remediation={}",
                    mailType,
                    failure.category(),
                    recipient,
                    configValue(smtpHost),
                    configValue(smtpPort),
                    elapsedMillis(startedAt),
                    rootCause.getClass().getSimpleName(),
                    summarizeMessage(rootCause.getMessage()),
                    failure.remediation(),
                    exception
            );
            throw new EmailDeliveryException(failure.category(), failure.userMessage(), exception);
        }
    }

    private MailFailure classifyMailFailure(Throwable throwable) {
        if (hasCause(throwable, MailAuthenticationException.class)
                || hasCause(throwable, AuthenticationFailedException.class)) {
            return new MailFailure(
                    "SMTP_AUTHENTICATION",
                    "Không thể xác thực máy chủ email. Vui lòng kiểm tra MAIL_USERNAME và MAIL_PASSWORD trên server.",
                    "Check that MAIL_USERNAME matches the Gmail account and MAIL_PASSWORD is an active Gmail App Password."
            );
        }

        if (hasCause(throwable, SocketTimeoutException.class)) {
            return new MailFailure(
                    "SMTP_TIMEOUT",
                    "Máy chủ email phản hồi quá chậm. Vui lòng thử lại sau.",
                    "Check outbound connectivity from Render to smtp.gmail.com:587. Increase SMTP timeouts only after confirming connectivity."
            );
        }

        if (hasCause(throwable, SSLHandshakeException.class)) {
            return new MailFailure(
                    "SMTP_TLS",
                    "Kết nối bảo mật SMTP thất bại. Vui lòng thử lại sau.",
                    "Check Gmail SMTP host, port 587, and STARTTLS settings."
            );
        }

        if (hasCause(throwable, ConnectException.class)) {
            return new MailFailure(
                    "SMTP_CONNECTION",
                    "Không thể kết nối đến máy chủ email. Vui lòng thử lại sau.",
                    "Check DNS and outbound access to smtp.gmail.com:587 from the production service."
            );
        }

        return new MailFailure(
                "SMTP_SEND",
                "Không thể gửi email OTP. Dịch vụ email tạm thời không khả dụng. Vui lòng thử lại sau.",
                "Inspect the root cause and stack trace in the SMTP_SEND_FAILED log event."
        );
    }

    private boolean hasCause(Throwable throwable, Class<? extends Throwable> expectedType) {
        Deque<Throwable> pending = new ArrayDeque<>();
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        pending.push(throwable);

        while (!pending.isEmpty()) {
            Throwable current = pending.pop();
            if (!visited.add(current)) {
                continue;
            }

            if (expectedType.isInstance(current)) {
                return true;
            }

            if (current.getCause() != null) {
                pending.push(current.getCause());
            }
            if (current instanceof MessagingException messagingException
                    && messagingException.getNextException() != null) {
                pending.push(messagingException.getNextException());
            }
        }
        return false;
    }

    private Throwable findRootCause(Throwable throwable) {
        Deque<Throwable> pending = new ArrayDeque<>();
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        Throwable deepest = throwable;
        pending.push(throwable);

        while (!pending.isEmpty()) {
            Throwable current = pending.pop();
            if (!visited.add(current)) {
                continue;
            }
            deepest = current;

            if (current.getCause() != null) {
                pending.push(current.getCause());
            }
            if (current instanceof MessagingException messagingException
                    && messagingException.getNextException() != null) {
                pending.push(messagingException.getNextException());
            }
        }
        return deepest;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String configValue(String value) {
        return hasText(value) ? value : "<unset>";
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }

    private String maskEmail(String email) {
        if (!hasText(email)) {
            return "<empty>";
        }

        String normalized = email.trim();
        int atIndex = normalized.indexOf('@');
        if (atIndex <= 0) {
            return "***";
        }

        String localPart = normalized.substring(0, atIndex);
        String visiblePrefix = localPart.substring(0, Math.min(2, localPart.length()));
        return visiblePrefix + "***" + normalized.substring(atIndex);
    }

    private String summarizeMessage(String message) {
        if (!hasText(message)) {
            return "<empty>";
        }

        String oneLine = message.replaceAll("[\\r\\n]+", " ");
        if (hasText(mailPassword)) {
            oneLine = oneLine.replace(mailPassword, "<redacted-password>");
        }
        if (hasText(fromEmail)) {
            oneLine = oneLine.replace(fromEmail, maskEmail(fromEmail));
        }
        return oneLine.length() <= 300 ? oneLine : oneLine.substring(0, 300) + "...";
    }

    private record MailFailure(String category, String userMessage, String remediation) {
    }

    private String buildRegisterOtpHtmlContent(String toEmail, String otpCode) {
        return "<!DOCTYPE html>"
                + "<html>"
                + "<head><meta charset='UTF-8'></head>"
                + "<body style='font-family: -apple-system, BlinkMacSystemFont, Segoe UI, Roboto, Helvetica, Arial, sans-serif; background-color: #f8fafc; margin: 0; padding: 30px; color: #1e293b;'>"
                + "  <div style='max-width: 520px; margin: 0 auto; background: #ffffff; border-radius: 16px; border: 1.5px solid #e2e8f0; overflow: hidden; box-shadow: 0 4px 14px rgba(0,0,0,0.05);'>"
                + "    <div style='background: linear-gradient(135deg, #16a34a 0%, #15803d 100%); padding: 24px; text-align: center; color: #ffffff;'>"
                + "      <h1 style='margin: 0; font-size: 22px; font-weight: 800; letter-spacing: 0.5px;'>Gia Sư HQ · Tutora</h1>"
                + "      <p style='margin: 6px 0 0 0; font-size: 13px; opacity: 0.9;'>Chào mừng bạn đến với Nền tảng kết nối Gia sư & Học sinh</p>"
                + "    </div>"
                + "    <div style='padding: 28px 24px;'>"
                + "      <h2 style='font-size: 18px; font-weight: 700; color: #0f172a; margin-top: 0;'>Xác thực tài khoản mới</h2>"
                + "      <p style='font-size: 14px; line-height: 1.6; color: #475569; margin-bottom: 20px;'>"
                + "        Xin chào,<br/>"
                + "        Bạn vừa tạo yêu cầu đăng ký tài khoản tại Gia Sư HQ với email <strong>" + toEmail + "</strong>. "
                + "        Vui lòng sử dụng mã OTP bên dưới để kích hoạt và bảo mật tài khoản của bạn:"
                + "      </p>"
                + "      <div style='background: #f0fdf4; border: 2px dashed #22c55e; border-radius: 12px; padding: 20px; text-align: center; margin: 24px 0;'>"
                + "        <div style='font-size: 12px; font-weight: 700; text-transform: uppercase; color: #15803d; margin-bottom: 6px; letter-spacing: 1px;'>Mã xác nhận kích hoạt</div>"
                + "        <span style='font-family: Consolas, Monaco, monospace; font-size: 36px; font-weight: 900; letter-spacing: 8px; color: #15803d;'>" + otpCode + "</span>"
                + "        <div style='font-size: 12px; color: #86efac; margin-top: 6px;'>Hiệu lực trong 10 phút</div>"
                + "      </div>"
                + "      <div style='background: #fffbeb; border-left: 4px solid #f59e0b; padding: 12px 14px; border-radius: 6px; font-size: 13px; color: #92400e; margin-bottom: 24px;'>"
                + "        <strong>Bảo mật:</strong> Tuyệt đối không chia sẻ mã này cho người khác để tránh bị chiếm đoạt tài khoản."
                + "      </div>"
                + "      <p style='font-size: 13px; line-height: 1.5; color: #64748b; margin: 0;'>"
                + "        Nếu bạn không thực hiện đăng ký này, bạn hoàn toàn có thể an tâm bỏ qua email.<br/><br/>"
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
