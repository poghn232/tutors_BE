package com.giasuhq.service.impl;

import com.giasuhq.dto.request.GoogleLoginRequest;
import com.giasuhq.dto.request.LoginRequest;
import com.giasuhq.dto.request.RegisterRequest;
import com.giasuhq.dto.response.AuthResponse;
import com.giasuhq.dto.response.GoogleUserInfo;
import com.giasuhq.dto.response.UserResponse;
import com.giasuhq.entity.Parent;
import com.giasuhq.entity.Role;
import com.giasuhq.entity.Tutor;
import com.giasuhq.entity.User;
import com.giasuhq.exception.ResourceNotFoundException;
import com.giasuhq.repository.UserRepository;
import com.giasuhq.security.JwtTokenProvider;
import com.giasuhq.service.AuthService;
import com.giasuhq.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;

    private static class OtpEntry {
        final String otp;
        final LocalDateTime expiryTime;

        OtpEntry(String otp, LocalDateTime expiryTime) {
            this.otp = otp;
            this.expiryTime = expiryTime;
        }

        boolean isExpired() {
            return LocalDateTime.now().isAfter(expiryTime);
        }
    }

    private final Map<String, OtpEntry> otpStorage = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
        if (userRepository.existsByEmailNormalized(email) || userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Email '" + email + "' đã được sử dụng trong hệ thống.");
        }

        // Verify registration OTP
        String otp = request.getOtp();
        if (otp == null || otp.trim().isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập mã OTP xác thực được gửi về Gmail của bạn.");
        }
        OtpEntry entry = otpStorage.get("REGISTER:" + email);
        if (entry == null || entry.isExpired()) {
            throw new IllegalArgumentException("Mã OTP không hợp lệ hoặc đã hết hạn (hiệu lực 10 phút). Vui lòng yêu cầu mã mới.");
        }
        if (!entry.otp.equals(otp.trim())) {
            throw new IllegalArgumentException("Mã OTP không chính xác. Vui lòng kiểm tra lại hộp thư.");
        }
        otpStorage.remove("REGISTER:" + email);

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        Role role = request.getRole() != null ? request.getRole() : Role.TUTOR;

        User user;
        if (role == Role.TUTOR) {
            user = Tutor.builder()
                    .email(email)
                    .password(encodedPassword)
                    .fullName(request.getFullName())
                    .phone(request.getPhone())
                    .role(Role.TUTOR)
                    .emailVerified(true)
                    .verificationStatus("PENDING")
                    .build();
        } else if (role == Role.PARENT) {
            user = Parent.builder()
                    .email(email)
                    .password(encodedPassword)
                    .fullName(request.getFullName())
                    .phone(request.getPhone())
                    .role(Role.PARENT)
                    .emailVerified(true)
                    .build();
        } else {
            user = User.builder()
                    .email(email)
                    .password(encodedPassword)
                    .fullName(request.getFullName())
                    .phone(request.getPhone())
                    .role(role != null ? role : Role.PARENT)
                    .emailVerified(true)
                    .build();
        }

        User savedUser = userRepository.save(user);
        String token = jwtTokenProvider.generateToken(savedUser.getEmail());

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .user(mapToUserResponse(savedUser))
                .build();
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
        User user = userRepository.findByEmailNormalized(email)
                .or(() -> userRepository.findByEmailIgnoreCase(email))
                .or(() -> userRepository.findByEmail(email))
                .orElseThrow(() -> new IllegalArgumentException("Email hoặc mật khẩu không chính xác."));

        boolean matches = passwordEncoder.matches(request.getPassword(), user.getPassword());

        // Self-healing: if password was stored as plain-text (legacy dev data)
        // or dummy seed placeholder "$2a$10$e.g123456hash" with default '123456'
        if (!matches) {
            if (user.getPassword() != null && user.getPassword().equals(request.getPassword())) {
                user.setPassword(passwordEncoder.encode(request.getPassword()));
                user = userRepository.save(user);
                matches = true;
            } else if ("$2a$10$e.g123456hash".equals(user.getPassword()) && "123456".equals(request.getPassword())) {
                user.setPassword(passwordEncoder.encode("123456"));
                user = userRepository.save(user);
                matches = true;
            }
        }

        if (!matches) {
            throw new IllegalArgumentException("Email hoặc mật khẩu không chính xác.");
        }

        if (user.getRole() == Role.STUDENT) {
            user.setRole(Role.PARENT);
            user = userRepository.save(user);
        }

        validateLoginPortal(request.getRole(), user.getRole());

        String token = jwtTokenProvider.generateToken(user.getEmail());

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .user(mapToUserResponse(user))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String email) {
        String normalizedEmail = email != null ? email.trim().toLowerCase() : "";
        User user = userRepository.findByEmailNormalized(normalizedEmail)
                .or(() -> userRepository.findByEmailIgnoreCase(normalizedEmail))
                .or(() -> userRepository.findByEmail(normalizedEmail))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin người dùng với email: " + email));
        return mapToUserResponse(user);
    }

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    @Transactional
    public AuthResponse loginWithGoogle(GoogleLoginRequest request) {
        String rawToken = request.getIdToken() != null ? request.getIdToken().trim() : "";
        String googleTokenUrl = rawToken.startsWith("ya29.")
                ? "https://oauth2.googleapis.com/tokeninfo?access_token=" + rawToken
                : "https://oauth2.googleapis.com/tokeninfo?id_token=" + rawToken;
        GoogleUserInfo userInfo;
        try {
            userInfo = restTemplate.getForObject(googleTokenUrl, GoogleUserInfo.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Token Google không hợp lệ hoặc đã hết hạn. Vui lòng thử lại.");
        }

        if (userInfo == null || userInfo.getEmail() == null || !"true".equalsIgnoreCase(userInfo.getEmailVerified())) {
            throw new IllegalArgumentException("Không thể xác thực tài khoản Google hoặc email chưa được xác minh.");
        }

        String email = userInfo.getEmail().trim().toLowerCase();
        Optional<User> existingUserOpt = userRepository.findByEmailNormalized(email)
                .or(() -> userRepository.findByEmailIgnoreCase(email))
                .or(() -> userRepository.findByEmail(email));

        User user;
        if (existingUserOpt.isPresent()) {
            user = existingUserOpt.get();
            validateLoginPortal(request.getRole(), user.getRole());
            if ((user.getAvatarUrl() == null || user.getAvatarUrl().isBlank()) && userInfo.getPicture() != null) {
                user.setAvatarUrl(userInfo.getPicture());
                user = userRepository.save(user);
            }
        } else {
            Role role = request.getRole() != null ? request.getRole() : Role.PARENT;
            String randomPassword = passwordEncoder.encode(UUID.randomUUID().toString());
            String fullName = (userInfo.getName() != null && !userInfo.getName().isBlank())
                    ? userInfo.getName()
                    : email.split("@")[0];

            if (role == Role.TUTOR) {
                user = Tutor.builder()
                        .email(email)
                        .password(randomPassword)
                        .fullName(fullName)
                        .avatarUrl(userInfo.getPicture())
                        .role(Role.TUTOR)
                        .emailVerified(true)
                        .verificationStatus("PENDING")
                        .build();
            } else {
                user = Parent.builder()
                        .email(email)
                        .password(randomPassword)
                        .fullName(fullName)
                        .avatarUrl(userInfo.getPicture())
                        .role(Role.PARENT)
                        .emailVerified(true)
                        .build();
            }
            user = userRepository.save(user);
        }

        String token = jwtTokenProvider.generateToken(user.getEmail());

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .user(mapToUserResponse(user))
                .build();
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .isVip(user.getIsVip() != null && user.getIsVip())
                .balance(user.getBalance())
                .emailVerified(user.getEmailVerified() != null ? user.getEmailVerified() : false)
                .createdAt(user.getCreatedAt())
                .build();
    }

    private void validateLoginPortal(Role requestedPortal, Role actualRole) {
        if (requestedPortal == null || actualRole == null || actualRole == Role.ADMIN) {
            return;
        }
        if (requestedPortal == Role.PARENT && actualRole == Role.STUDENT) {
            return;
        }
        if (requestedPortal != actualRole) {
            throw new IllegalArgumentException(
                    "Bạn không thể đăng nhập ở cổng " + getPortalDisplayName(requestedPortal)
                            + ". Tài khoản của bạn được đăng ký với vai trò "
                            + getRoleDisplayName(actualRole) + "."
            );
        }
    }

    private String getPortalDisplayName(Role portal) {
        if (portal == null) return "Chưa xác định";
        switch (portal) {
            case TUTOR: return "Gia sư";
            case PARENT: return "Phụ huynh";
            case STUDENT: return "Phụ huynh / Học sinh";
            default: return portal.name();
        }
    }

    private String getRoleDisplayName(Role role) {
        if (role == null) return "Chưa xác định";
        switch (role) {
            case TUTOR: return "Gia sư";
            case PARENT: return "Phụ huynh";
            case ADMIN: return "Quản trị viên";
            case STUDENT: return "Phụ huynh / Học sinh";
            default: return role.name();
        }
    }

    @Override
    public String sendRegisterOtp(String email, String fullName) {
        String normEmail = email != null ? email.trim().toLowerCase() : "";
        if (normEmail.isBlank() || !normEmail.contains("@")) {
            throw new IllegalArgumentException("Vui lòng cung cấp địa chỉ email hợp lệ.");
        }

        if (userRepository.existsByEmailIgnoreCase(normEmail) || userRepository.existsByEmailNormalized(normEmail)) {
            throw new IllegalArgumentException("Email '" + normEmail + "' đã được sử dụng. Vui lòng đăng nhập hoặc dùng email khác.");
        }

        int randomPin = new SecureRandom().nextInt(900000) + 100000;
        String otp = String.valueOf(randomPin);

        otpStorage.put("REGISTER:" + normEmail, new OtpEntry(otp, LocalDateTime.now().plusMinutes(10)));
        log.info("Generated registration OTP for {}: [{}]", normEmail, otp);

        emailService.sendRegisterOtpEmail(normEmail, otp);
        return emailService.isMailConfigured() ? null : otp;
    }

    @Override
    public String sendForgotPasswordOtp(String email) {
        String normEmail = email != null ? email.trim().toLowerCase() : "";
        if (normEmail.isBlank()) {
            throw new IllegalArgumentException("Vui lòng cung cấp địa chỉ email hợp lệ.");
        }

        User user = userRepository.findByEmailNormalized(normEmail)
                .or(() -> userRepository.findByEmailIgnoreCase(normEmail))
                .or(() -> userRepository.findByEmail(normEmail))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản liên kết với email: " + normEmail));

        // Auto-heal legacy STUDENT role to PARENT
        if (user.getRole() == Role.STUDENT) {
            user.setRole(Role.PARENT);
            userRepository.save(user);
        }

        int randomPin = new SecureRandom().nextInt(900000) + 100000;
        String otp = String.valueOf(randomPin);

        otpStorage.put(normEmail, new OtpEntry(otp, LocalDateTime.now().plusMinutes(10)));
        log.info("Generated forgot-password OTP for {}: [{}]", normEmail, otp);

        emailService.sendOtpEmail(normEmail, otp);
        return emailService.isMailConfigured() ? null : otp;
    }

    @Override
    public void verifyOtp(String email, String otp) {
        String normEmail = email != null ? email.trim().toLowerCase() : "";
        if (normEmail.isBlank()) {
            throw new IllegalArgumentException("Vui lòng cung cấp email.");
        }
        if (otp == null || otp.trim().isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập mã OTP xác thực.");
        }

        OtpEntry entry = otpStorage.get(normEmail);
        if (entry == null || entry.isExpired()) {
            throw new IllegalArgumentException("Mã OTP không hợp lệ hoặc đã hết hạn (hiệu lực 10 phút). Vui lòng yêu cầu mã mới.");
        }

        if (!entry.otp.equals(otp.trim())) {
            throw new IllegalArgumentException("Mã OTP không chính xác. Vui lòng kiểm tra lại hộp thư.");
        }
    }

    @Override
    @Transactional
    public void resetPassword(String email, String otp, String newPassword) {
        verifyOtp(email, otp);

        String normEmail = email.trim().toLowerCase();
        User user = userRepository.findByEmailNormalized(normEmail)
                .or(() -> userRepository.findByEmailIgnoreCase(normEmail))
                .or(() -> userRepository.findByEmail(normEmail))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản với email: " + normEmail));

        if (user.getRole() == Role.STUDENT) {
            user.setRole(Role.PARENT);
        }

        if (newPassword == null || newPassword.trim().length() < 6) {
            throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 6 ký tự.");
        }

        user.setPassword(passwordEncoder.encode(newPassword.trim()));
        userRepository.save(user);

        // Invalidate OTP after successful reset
        otpStorage.remove(normEmail);
        log.info("Password successfully reset for user: {}", normEmail);
    }
}
