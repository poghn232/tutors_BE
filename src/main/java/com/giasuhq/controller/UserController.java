package com.giasuhq.controller;

import com.giasuhq.dto.request.UpdateProfileRequest;
import com.giasuhq.dto.response.ApiResponse;
import com.giasuhq.dto.response.UserProfileResponse;
import com.giasuhq.entity.User;
import com.giasuhq.repository.UserRepository;
import com.giasuhq.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

    @GetMapping("/profile")
    public ApiResponse<UserProfileResponse> getProfile(Principal principal) {
        User user = getUserByPrincipal(principal);
        UserProfileResponse response = userService.getUserProfile(user);
        return ApiResponse.success("Lấy thông tin hồ sơ thành công", response);
    }

    @PutMapping("/profile")
    public ApiResponse<UserProfileResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request, Principal principal) {
        User user = getUserByPrincipal(principal);
        UserProfileResponse response = userService.updateProfile(user, request);
        return ApiResponse.success("Cập nhật hồ sơ thành công!", response);
    }

    private User getUserByPrincipal(Principal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("Chưa đăng nhập.");
        }
        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng."));
    }
}
