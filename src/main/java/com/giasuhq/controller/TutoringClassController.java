package com.giasuhq.controller;

import com.giasuhq.dto.request.CreateClassRequest;
import com.giasuhq.dto.response.ApiResponse;
import com.giasuhq.dto.response.ClassResponse;
import com.giasuhq.entity.User;
import com.giasuhq.repository.UserRepository;
import com.giasuhq.service.TutoringClassService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
public class TutoringClassController {

    private final TutoringClassService tutoringClassService;
    private final UserRepository userRepository;

    @GetMapping
    public ApiResponse<List<ClassResponse>> getClasses(Principal principal) {
        User user = getUserByPrincipal(principal);
        List<ClassResponse> classes = tutoringClassService.getClassesForUser(user);
        return ApiResponse.success("Lấy danh sách lớp học thành công", classes);
    }

    @GetMapping("/{id}")
    public ApiResponse<ClassResponse> getClassById(@PathVariable Long id, Principal principal) {
        User user = getUserByPrincipal(principal);
        ClassResponse response = tutoringClassService.getClassById(id, user);
        return ApiResponse.success("Lấy thông tin lớp học thành công", response);
    }

    @PostMapping
    public ApiResponse<ClassResponse> createClass(@Valid @RequestBody CreateClassRequest request, Principal principal) {
        User user = getUserByPrincipal(principal);
        ClassResponse response = tutoringClassService.createClass(request, user);
        return ApiResponse.success("Tạo lớp học mới thành công!", response);
    }

    private User getUserByPrincipal(Principal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("Chưa đăng nhập.");
        }
        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng."));
    }
}
