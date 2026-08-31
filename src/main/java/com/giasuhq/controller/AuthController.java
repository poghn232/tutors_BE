package com.giasuhq.controller;

import com.giasuhq.dto.request.LoginRequest;
import com.giasuhq.dto.request.RegisterRequest;
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

    @GetMapping("/me")
    public ApiResponse<UserResponse> getCurrentUser(Principal principal) {
        if (principal == null) {
            return ApiResponse.error("Chưa đăng nhập hoặc phiên làm việc hết hạn");
        }
        UserResponse response = authService.getCurrentUser(principal.getName());
        return ApiResponse.success("Lấy thông tin người dùng thành công", response);
    }
}
