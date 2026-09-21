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
        User user = null;
        if (principal != null) {
            try {
                user = getUserByPrincipal(principal);
            } catch (Exception e) {
                // principal exists but user not found in DB
            }
        }
        // Fallback: if principal was null or user not found, try studentId from request
        if (user == null && request.getStudentId() != null) {
            user = userRepository.findById(request.getStudentId()).orElse(null);
        }
        // Fallback: try studentEmail from request
        if (user == null && request.getStudentEmail() != null && !request.getStudentEmail().isBlank()) {
            user = userRepository.findByEmailIgnoreCase(request.getStudentEmail())
                    .or(() -> userRepository.findByEmail(request.getStudentEmail()))
                    .orElse(null);
        }
        if (user == null) {
            throw new IllegalArgumentException("Chưa đăng nhập hoặc phiên làm việc đã hết hạn. Vui lòng đăng nhập lại.");
        }
        ClassResponse response = tutoringClassService.createClass(request, user);
        return ApiResponse.success("Đã gửi yêu cầu kết nối. Vui lòng chờ gia sư chấp nhận lịch học.", response);
    }

    @PostMapping("/{id}/accept")
    public ApiResponse<ClassResponse> acceptClass(@PathVariable Long id, Principal principal) {
        User user = getUserByPrincipal(principal);
        ClassResponse response = tutoringClassService.acceptClass(id, user);
        return ApiResponse.success("Gia sư đã chấp nhận lịch học. Phụ huynh/học sinh có thể thanh toán phí kết nối.", response);
    }

    @PostMapping("/{id}/decline")
    public ApiResponse<ClassResponse> declineClass(@PathVariable Long id, Principal principal) {
        User user = getUserByPrincipal(principal);
        ClassResponse response = tutoringClassService.declineClass(id, user);
        return ApiResponse.success("Gia sư đã từ chối yêu cầu kết nối.", response);
    }

    @PostMapping("/{id}/pay-connection-fee")
    public ApiResponse<ClassResponse> payConnectionFee(@PathVariable Long id, Principal principal) {
        User user = getUserByPrincipal(principal);
        ClassResponse response = tutoringClassService.payConnectionFee(id, user);
        return ApiResponse.success("Thanh toán phí kết nối thành công. Lớp học đã được kích hoạt.", response);
    }

    private User getUserByPrincipal(Principal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("Chưa đăng nhập hoặc phiên làm việc đã hết hạn.");
        }
        String email = principal.getName();
        return userRepository.findByEmailIgnoreCase(email)
                .or(() -> userRepository.findByEmail(email))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng với tài khoản: " + email));
    }
}
