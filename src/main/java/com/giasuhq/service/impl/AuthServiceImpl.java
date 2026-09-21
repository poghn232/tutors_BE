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
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
        if (userRepository.existsByEmailNormalized(email) || userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Email '" + email + "' đã được sử dụng trong hệ thống.");
        }

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
                    .build();
        } else {
            user = Parent.builder()
                    .email(email)
                    .password(encodedPassword)
                    .fullName(request.getFullName())
                    .phone(request.getPhone())
                    .role(Role.PARENT)
                    .build();
        } else {
            user = User.builder()
                    .email(email)
                    .password(encodedPassword)
                    .fullName(request.getFullName())
                    .phone(request.getPhone())
                    .role(role != null ? role : Role.PARENT)
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
        if (request.getRole() != null && user.getRole() != request.getRole()) {
            throw new IllegalArgumentException("Tài khoản này có vai trò là " + getRoleDisplayName(user.getRole()) + ", không thể đăng nhập ở cổng " + getRoleDisplayName(request.getRole()) + ".");
        }

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
            if (request.getRole() != null && user.getRole() != request.getRole()) {
                throw new IllegalArgumentException("Tài khoản này có vai trò là " + getRoleDisplayName(user.getRole()) + ", không thể đăng nhập ở cổng " + getRoleDisplayName(request.getRole()) + ".");
            }
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
                        .build();
            } else {
                user = Parent.builder()
                        .email(email)
                        .password(randomPassword)
                        .fullName(fullName)
                        .avatarUrl(userInfo.getPicture())
                        .role(Role.PARENT)
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
                .createdAt(user.getCreatedAt())
                .build();
    }

    private String getRoleDisplayName(Role role) {
        if (role == null) return "Chưa xác định";
        switch (role) {
            case TUTOR: return "Gia sư";
            case PARENT: return "Phụ huynh";
            case ADMIN: return "Quản trị viên";
            default: return role.name();
        }
    }
}
