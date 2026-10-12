package com.giasuhq.controller;

import com.giasuhq.dto.request.ProgressReportRequest;
import com.giasuhq.dto.response.ApiResponse;
import com.giasuhq.dto.response.ProgressReportResponse;
import com.giasuhq.entity.User;
import com.giasuhq.repository.UserRepository;
import com.giasuhq.service.ProgressReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/progress")
@RequiredArgsConstructor
public class ProgressReportController {

    private final ProgressReportService progressReportService;
    private final UserRepository userRepository;

    @GetMapping("/report")
    public ApiResponse<ProgressReportResponse> getReport(
            @RequestParam(value = "classId", required = false) Long classId,
            Principal principal
    ) {
        User user = getUserByPrincipal(principal);
        return ApiResponse.success("Tạo báo cáo tiến độ thành công", progressReportService.generateReport(user, classId));
    }

    @PostMapping("/report")
    public ApiResponse<ProgressReportResponse> generateReport(
            @Valid @RequestBody(required = false) ProgressReportRequest request,
            Principal principal
    ) {
        User user = getUserByPrincipal(principal);
        Long classId = request != null ? request.getClassId() : null;
        return ApiResponse.success("AI đã tạo báo cáo tiến độ thành công", progressReportService.generateReport(user, classId));
    }

    private User getUserByPrincipal(Principal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("Vui lòng đăng nhập để xem báo cáo tiến độ.");
        }
        return userRepository.findByEmailIgnoreCase(principal.getName())
                .or(() -> userRepository.findByEmail(principal.getName()))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng hiện tại."));
    }
}
