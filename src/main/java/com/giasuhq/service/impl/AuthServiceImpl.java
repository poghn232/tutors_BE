package com.giasuhq.service.impl;

import com.giasuhq.dto.request.LoginRequest;
import com.giasuhq.dto.request.RegisterRequest;
import com.giasuhq.dto.response.AuthResponse;
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
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin người dùng với email: " + email));
        return mapToUserResponse(user);
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
