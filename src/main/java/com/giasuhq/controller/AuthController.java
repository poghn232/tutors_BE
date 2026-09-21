package com.giasuhq.controller;

import com.giasuhq.dto.request.*;
import com.giasuhq.dto.response.ApiResponse;
import com.giasuhq.dto.response.AuthResponse;
import com.giasuhq.dto.response.UserResponse;
import com.giasuhq.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/send-register-otp")
    public ApiResponse<Void> sendRegisterOtp(@Valid @RequestBody SendRegisterOtpRequest request) {
        authService.sendRegisterOtp(request.getEmail(), request.getFullName());
        return ApiResponse.success("Mã xác thực kích hoạt tài khoản đã được gửi về Gmail của bạn. Vui lòng kiểm tra hộp thư!", null);
    }

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ApiResponse.success("Đăng ký tài khoản thành công!", response);
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ApiResponse.success("Đăng nhập thành công!", response);
    }

    @PostMapping("/google")
    public ApiResponse<AuthResponse> loginWithGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        AuthResponse response = authService.loginWithGoogle(request);
        return ApiResponse.success("Đăng nhập bằng Google thành công!", response);
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.sendForgotPasswordOtp(request.getEmail());
        return ApiResponse.success("Mã xác minh OTP đã được gửi về email của bạn. Vui lòng kiểm tra hộp thư!", null);
    }

    @PostMapping("/verify-otp")
    public ApiResponse<Void> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        authService.verifyOtp(request.getEmail(), request.getOtp());
        return ApiResponse.success("Xác thực mã OTP thành công! Bạn có thể đặt mật khẩu mới.", null);
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.getEmail(), request.getOtp(), request.getNewPassword());
        return ApiResponse.success("Đặt lại mật khẩu thành công! Bạn có thể đăng nhập bằng mật khẩu mới.", null);
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> getCurrentUser(Principal principal) {
        if (principal == null) {
            return ApiResponse.error("Chưa đăng nhập hoặc phiên làm việc hết hạn");
        }
        UserResponse response = authService.getCurrentUser(principal.getName());
        return ApiResponse.success("Lấy thông tin người dùng thành công", response);
    }
}
