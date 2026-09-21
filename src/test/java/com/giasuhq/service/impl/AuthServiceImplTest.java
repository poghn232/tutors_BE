package com.giasuhq.service.impl;

import com.giasuhq.dto.request.LoginRequest;
import com.giasuhq.dto.response.AuthResponse;
import com.giasuhq.entity.Role;
import com.giasuhq.entity.User;
import com.giasuhq.repository.UserRepository;
import com.giasuhq.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void shouldRejectTutorAccountLoggingInThroughParentPortal() {
        User tutorUser = User.builder()
                .id(1L)
                .email("giasu@gmail.com")
                .password("encoded_pass")
                .role(Role.TUTOR)
                .build();

        when(userRepository.findByEmailNormalized("giasu@gmail.com")).thenReturn(Optional.of(tutorUser));
        when(passwordEncoder.matches("123456", "encoded_pass")).thenReturn(true);

        LoginRequest request = LoginRequest.builder()
                .email("giasu@gmail.com")
                .password("123456")
                .role(Role.PARENT) // User chose Parent card on login page
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.login(request));
        assertTrue(ex.getMessage().contains("Tài khoản này có vai trò là Gia sư, không thể đăng nhập ở cổng Phụ huynh"));
    }

    @Test
    void shouldRejectParentAccountLoggingInThroughTutorPortal() {
        User parentUser = User.builder()
                .id(2L)
                .email("phuhuynh@gmail.com")
                .password("encoded_pass")
                .role(Role.PARENT)
                .build();

        when(userRepository.findByEmailNormalized("phuhuynh@gmail.com")).thenReturn(Optional.of(parentUser));
        when(passwordEncoder.matches("123456", "encoded_pass")).thenReturn(true);

        LoginRequest request = LoginRequest.builder()
                .email("phuhuynh@gmail.com")
                .password("123456")
                .role(Role.TUTOR) // User chose Tutor card on login page
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.login(request));
        assertTrue(ex.getMessage().contains("Tài khoản này có vai trò là Phụ huynh, không thể đăng nhập ở cổng Gia sư"));
    }

    @Test
    void shouldAllowTutorAccountLoggingInThroughTutorPortal() {
        User tutorUser = User.builder()
                .id(1L)
                .email("giasu@gmail.com")
                .password("encoded_pass")
                .fullName("Nguyễn Văn Gia Sư")
                .role(Role.TUTOR)
                .build();

        when(userRepository.findByEmailNormalized("giasu@gmail.com")).thenReturn(Optional.of(tutorUser));
        when(passwordEncoder.matches("123456", "encoded_pass")).thenReturn(true);
        when(jwtTokenProvider.generateToken("giasu@gmail.com")).thenReturn("mock_token");

        LoginRequest request = LoginRequest.builder()
                .email("giasu@gmail.com")
                .password("123456")
                .role(Role.TUTOR)
                .build();

        AuthResponse response = authService.login(request);
        assertNotNull(response);
        assertEquals("mock_token", response.getToken());
        assertEquals(Role.TUTOR, response.getUser().getRole());
    }

    @Test
    void shouldAllowAdminToLoginViaAnyPortal() {
        User adminUser = User.builder()
                .id(99L)
                .email("admin@giasuhq.com")
                .password("encoded_pass")
                .fullName("Quản trị viên")
                .role(Role.ADMIN)
                .build();

        when(userRepository.findByEmailNormalized("admin@giasuhq.com")).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.matches("admin123", "encoded_pass")).thenReturn(true);
        when(jwtTokenProvider.generateToken("admin@giasuhq.com")).thenReturn("admin_jwt_token");

        LoginRequest request = LoginRequest.builder()
                .email("admin@giasuhq.com")
                .password("admin123")
                .role(Role.PARENT) // Admin logging in via Parent card
                .build();

        AuthResponse response = authService.login(request);
        assertNotNull(response);
        assertEquals(Role.ADMIN, response.getUser().getRole());
    }
}
