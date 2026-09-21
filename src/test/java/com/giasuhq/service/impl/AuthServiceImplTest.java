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
    void shouldAuthenticateTutorAndReturnTutorRoleFromDatabase() {
        User tutorUser = User.builder()
                .id(1L)
                .email("giasu@gmail.com")
                .password("encoded_pass")
                .fullName("Nguyễn Văn Gia Sư")
                .role(Role.TUTOR)
                .build();

        when(userRepository.findByEmailNormalized("giasu@gmail.com")).thenReturn(Optional.of(tutorUser));
        when(passwordEncoder.matches("123456", "encoded_pass")).thenReturn(true);
        when(jwtTokenProvider.generateToken("giasu@gmail.com")).thenReturn("mock_tutor_token");

        LoginRequest request = LoginRequest.builder()
                .email("giasu@gmail.com")
                .password("123456")
                .build();

        AuthResponse response = authService.login(request);
        assertNotNull(response);
        assertEquals("mock_tutor_token", response.getToken());
        assertEquals(Role.TUTOR, response.getUser().getRole());
    }

    @Test
    void shouldAuthenticateParentAndReturnParentRoleFromDatabase() {
        User parentUser = User.builder()
                .id(2L)
                .email("phuhuynh@gmail.com")
                .password("encoded_pass")
                .fullName("Trần Thị Phụ Huynh")
                .role(Role.PARENT)
                .build();

        when(userRepository.findByEmailNormalized("phuhuynh@gmail.com")).thenReturn(Optional.of(parentUser));
        when(passwordEncoder.matches("123456", "encoded_pass")).thenReturn(true);
        when(jwtTokenProvider.generateToken("phuhuynh@gmail.com")).thenReturn("mock_parent_token");

        LoginRequest request = LoginRequest.builder()
                .email("phuhuynh@gmail.com")
                .password("123456")
                .build();

        AuthResponse response = authService.login(request);
        assertNotNull(response);
        assertEquals("mock_parent_token", response.getToken());
        assertEquals(Role.PARENT, response.getUser().getRole());
    }

    @Test
    void shouldAllowAdminToLoginAndReturnAdminRoleFromDatabase() {
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
                .build();

        AuthResponse response = authService.login(request);
        assertNotNull(response);
        assertEquals(Role.ADMIN, response.getUser().getRole());
    }

    @Test
    void shouldRejectTutorLoggingInThroughParentPortal() {
        User tutorUser = User.builder()
                .id(1L)
                .email("giasu@gmail.com")
                .password("encoded_pass")
                .fullName("Nguyễn Văn Gia Sư")
                .role(Role.TUTOR)
                .build();

        when(userRepository.findByEmailNormalized("giasu@gmail.com")).thenReturn(Optional.of(tutorUser));
        when(passwordEncoder.matches("123456", "encoded_pass")).thenReturn(true);

        LoginRequest request = LoginRequest.builder()
                .email("giasu@gmail.com")
                .password("123456")
                .role(Role.PARENT)
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.login(request));
        assertTrue(ex.getMessage().contains("không thể đăng nhập ở cổng"));
    }

    @Test
    void shouldRejectParentLoggingInThroughTutorPortal() {
        User parentUser = User.builder()
                .id(2L)
                .email("phuhuynh@gmail.com")
                .password("encoded_pass")
                .fullName("Trần Thị Phụ Huynh")
                .role(Role.PARENT)
                .build();

        when(userRepository.findByEmailNormalized("phuhuynh@gmail.com")).thenReturn(Optional.of(parentUser));
        when(passwordEncoder.matches("123456", "encoded_pass")).thenReturn(true);

        LoginRequest request = LoginRequest.builder()
                .email("phuhuynh@gmail.com")
                .password("123456")
                .role(Role.TUTOR)
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.login(request));
        assertTrue(ex.getMessage().contains("không thể đăng nhập ở cổng"));
    }

    @Test
    void shouldRejectInvalidPassword() {
        User user = User.builder()
                .id(1L)
                .email("test@gmail.com")
                .password("encoded_pass")
                .role(Role.TUTOR)
                .build();

        when(userRepository.findByEmailNormalized("test@gmail.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong_pass", "encoded_pass")).thenReturn(false);

        LoginRequest request = LoginRequest.builder()
                .email("test@gmail.com")
                .password("wrong_pass")
                .build();

        assertThrows(IllegalArgumentException.class, () -> authService.login(request));
    }
}
