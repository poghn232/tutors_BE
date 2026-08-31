package com.giasuhq.service;

import com.giasuhq.dto.request.LoginRequest;
import com.giasuhq.dto.request.RegisterRequest;
import com.giasuhq.dto.response.AuthResponse;
import com.giasuhq.dto.response.UserResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    UserResponse getCurrentUser(String email);
}
