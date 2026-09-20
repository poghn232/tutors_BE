package com.giasuhq.service.impl;

import com.giasuhq.dto.request.GoogleLoginRequest;
import com.giasuhq.dto.request.LoginRequest;
import com.giasuhq.dto.request.RegisterRequest;
import com.giasuhq.dto.response.AuthResponse;
import com.giasuhq.dto.response.GoogleUserInfo;
import com.giasuhq.dto.response.UserResponse;
import com.giasuhq.entity.Parent;
import com.giasuhq.entity.Role;
import com.giasuhq.entity.Student;
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
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email '" + request.getEmail() + "' đã được sử dụng trong hệ thống.");
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        Role role = request.getRole() != null ? request.getRole() : Role.TUTOR;

        User user;
        if (role == Role.TUTOR) {
            user = Tutor.builder()
                    .email(request.getEmail())
                    .password(encodedPassword)
                    .fullName(request.getFullName())
                    .phone(request.getPhone())
                    .role(Role.TUTOR)
                    .build();
        } else if (role == Role.PARENT) {
            user = Parent.builder()
                    .email(request.getEmail())
                    .password(encodedPassword)
                    .fullName(request.getFullName())
                    .phone(request.getPhone())
                    .role(Role.PARENT)
                    .build();
        } else if (role == Role.STUDENT) {
            user = Student.builder()
                    .email(request.getEmail())
                    .password(encodedPassword)
                    .fullName(request.getFullName())
                    .phone(request.getPhone())
                    .role(Role.STUDENT)
                    .build();
        } else {
            user = User.builder()
                    .email(request.getEmail())
                    .password(encodedPassword)
                    .fullName(request.getFullName())
                    .phone(request.getPhone())
                    .role(role)
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
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Email hoặc mật khẩu không chính xác."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Email hoặc mật khẩu không chính xác.");
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
        User user = userRepository.findByEmailIgnoreCase(email)
                .or(() -> userRepository.findByEmail(email))
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
        Optional<User> existingUserOpt = userRepository.findByEmail(email);

        User user;
        if (existingUserOpt.isPresent()) {
            user = existingUserOpt.get();
            if ((user.getAvatarUrl() == null || user.getAvatarUrl().isBlank()) && userInfo.getPicture() != null) {
                user.setAvatarUrl(userInfo.getPicture());
                user = userRepository.save(user);
            }
        } else {
            Role role = request.getRole() != null ? request.getRole() : Role.STUDENT;
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
            } else if (role == Role.PARENT) {
                user = Parent.builder()
                        .email(email)
                        .password(randomPassword)
                        .fullName(fullName)
                        .avatarUrl(userInfo.getPicture())
                        .role(Role.PARENT)
                        .build();
            } else {
                user = Student.builder()
                        .email(email)
                        .password(randomPassword)
                        .fullName(fullName)
                        .avatarUrl(userInfo.getPicture())
                        .role(Role.STUDENT)
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
                .createdAt(user.getCreatedAt())
                .build();
    }
}
