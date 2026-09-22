package com.giasuhq.controller;

import com.giasuhq.dto.request.CreateAssignmentRequest;
import com.giasuhq.dto.request.GradeAssignmentRequest;
import com.giasuhq.dto.request.SubmitAssignmentRequest;
import com.giasuhq.dto.response.ApiResponse;
import com.giasuhq.dto.response.AssignmentResponse;
import com.giasuhq.dto.response.ParentSummaryResponse;
import com.giasuhq.entity.User;
import com.giasuhq.repository.UserRepository;
import com.giasuhq.service.AssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/assignments")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService assignmentService;
    private final UserRepository userRepository;

    @GetMapping
    public ApiResponse<List<AssignmentResponse>> getAssignments(Principal principal) {
        User user = getUserByPrincipal(principal);
        List<AssignmentResponse> responses = assignmentService.getAssignmentsForUser(user);
        return ApiResponse.success("Lấy danh sách bài tập thành công", responses);
    }

    @GetMapping("/parents/search")
    public ApiResponse<List<ParentSummaryResponse>> searchParents(@RequestParam(value = "query", required = false) String query) {
        List<ParentSummaryResponse> responses = assignmentService.searchParents(query);
        return ApiResponse.success("Tìm kiếm phụ huynh thành công", responses);
    }

    @GetMapping("/{id}")
    public ApiResponse<AssignmentResponse> getAssignmentById(@PathVariable Long id, Principal principal) {
        User user = getUserByPrincipal(principal);
        AssignmentResponse response = assignmentService.getAssignmentById(id, user);
        return ApiResponse.success("Lấy chi tiết bài tập thành công", response);
    }

    @PostMapping
    public ApiResponse<AssignmentResponse> createAssignment(@Valid @RequestBody CreateAssignmentRequest request, Principal principal) {
        User user = getUserByPrincipal(principal);
        AssignmentResponse response = assignmentService.createAssignment(user, request);
        return ApiResponse.success("Giao bài tập mới thành công!", response);
    }

    @PostMapping("/{id}/submit")
    public ApiResponse<AssignmentResponse> submitAssignment(@PathVariable Long id, @Valid @RequestBody SubmitAssignmentRequest request, Principal principal) {
        User user = getUserByPrincipal(principal);
        AssignmentResponse response = assignmentService.submitAssignment(user, id, request);
        return ApiResponse.success("Nộp bài tập thành công!", response);
    }

    @PostMapping("/{id}/grade")
    public ApiResponse<AssignmentResponse> gradeAssignment(@PathVariable Long id, @Valid @RequestBody GradeAssignmentRequest request, Principal principal) {
        User user = getUserByPrincipal(principal);
        AssignmentResponse response = assignmentService.gradeAssignment(user, id, request);
        return ApiResponse.success("Đã lưu chấm điểm và nhận xét thành công!", response);
    }

    private User getUserByPrincipal(Principal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("Chưa đăng nhập hoặc phiên làm việc đã hết hạn. Vui lòng đăng nhập lại.");
        }
        String email = principal.getName();
        return userRepository.findByEmailIgnoreCase(email)
                .or(() -> userRepository.findByEmail(email))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng với tài khoản: " + email));
    }
}
