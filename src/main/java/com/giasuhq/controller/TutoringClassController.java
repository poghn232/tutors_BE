package com.giasuhq.controller;

import com.giasuhq.dto.request.CreateClassRequest;
import com.giasuhq.dto.request.ConnectionPaymentRequest;
import com.giasuhq.dto.response.ApiResponse;
import com.giasuhq.dto.response.ClassResponse;
import com.giasuhq.dto.response.ConnectionPaymentResponse;
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
        return ApiResponse.success("Đã gửi yêu cầu kết nối. Vui lòng chờ gia sư chấp nhận lịch học.", response);
    }

    @PostMapping("/{id}/accept")
    public ApiResponse<ClassResponse> acceptClass(@PathVariable Long id, Principal principal) {
        User user = getUserByPrincipal(principal);
        ClassResponse response = tutoringClassService.acceptClass(id, user);
        return ApiResponse.success("Gia sư đã xác nhận lịch học. Phí kết nối ban đầu đã được ghi nhận và lớp học đã kích hoạt.", response);
    }

    @PostMapping("/{id}/decline")
    public ApiResponse<ClassResponse> declineClass(@PathVariable Long id, Principal principal) {
        User user = getUserByPrincipal(principal);
        ClassResponse response = tutoringClassService.declineClass(id, user);
        return ApiResponse.success("Gia sư đã từ chối yêu cầu kết nối.", response);
    }

    @PostMapping("/{id}/pay-connection-fee")
    public ApiResponse<ConnectionPaymentResponse> payConnectionFee(
            @PathVariable Long id,
            @RequestBody(required = false) ConnectionPaymentRequest request,
            Principal principal
    ) {
        User user = getUserByPrincipal(principal);
        String voucherCode = request != null ? request.getVoucherCode() : null;
        ConnectionPaymentResponse response = tutoringClassService.payConnectionFee(id, user, voucherCode);
        String message = response.getIssuedVoucher() != null
                ? "Thanh toán phí kết nối ban đầu thành công. Bạn được tặng voucher " + response.getIssuedVoucher().getCode() + "."
                : "Phí kết nối ban đầu đã được ghi nhận. Không cần thanh toán lại.";
        return ApiResponse.success(message, response);
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
