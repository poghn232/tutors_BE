package com.giasuhq.controller;

import com.giasuhq.dto.request.ApplyTutorPostRequest;
import com.giasuhq.dto.request.CreateTutorPostRequest;
import com.giasuhq.dto.request.RejectApplicationRequest;
import com.giasuhq.dto.request.UpdateTutorPostRequest;
import com.giasuhq.dto.response.ApiResponse;
import com.giasuhq.dto.response.TutorApplicationResponse;
import com.giasuhq.dto.response.TutorPostResponse;
import com.giasuhq.entity.TeachingMode;
import com.giasuhq.entity.User;
import com.giasuhq.repository.UserRepository;
import com.giasuhq.service.TutorApplicationService;
import com.giasuhq.service.TutorPostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/tutor-posts")
@RequiredArgsConstructor
public class TutorPostController {

    private final TutorPostService tutorPostService;
    private final TutorApplicationService tutorApplicationService;
    private final UserRepository userRepository;

    @PostMapping
    public ApiResponse<TutorPostResponse> createPost(
            @Valid @RequestBody CreateTutorPostRequest request,
            Principal principal
    ) {
        User user = getUserByPrincipal(principal);
        TutorPostResponse response = tutorPostService.createPost(request, user);
        return ApiResponse.success("Đăng bài tìm gia sư thành công. Bài đăng sẽ được hiển thị sau khi Admin duyệt.", response);
    }

    @GetMapping
    public ApiResponse<Page<TutorPostResponse>> getPublicPosts(
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) String gradeLevel,
            @RequestParam(required = false) TeachingMode teachingMode,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) BigDecimal minBudget,
            @RequestParam(required = false) BigDecimal maxBudget,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Principal principal
    ) {
        User user = principal != null ? getUserByPrincipal(principal) : null;
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(size, 50));
        Page<TutorPostResponse> posts = tutorPostService.searchPublicPosts(
                subjectId, gradeLevel, teachingMode, city, district, minBudget, maxBudget, keyword, user, pageable
        );
        return ApiResponse.success("Lấy danh sách bài đăng tìm gia sư thành công", posts);
    }

    @GetMapping("/my-posts")
    public ApiResponse<List<TutorPostResponse>> getMyPosts(Principal principal) {
        User user = getUserByPrincipal(principal);
        List<TutorPostResponse> posts = tutorPostService.getMyPosts(user);
        return ApiResponse.success("Lấy danh sách bài đăng của bạn thành công", posts);
    }

    @GetMapping("/my-applications")
    public ApiResponse<List<TutorApplicationResponse>> getMyApplications(Principal principal) {
        User user = getUserByPrincipal(principal);
        List<TutorApplicationResponse> apps = tutorApplicationService.getMyApplications(user);
        return ApiResponse.success("Lấy danh sách đơn ứng tuyển của bạn thành công", apps);
    }

    @GetMapping("/{id}")
    public ApiResponse<TutorPostResponse> getPostById(@PathVariable Long id, Principal principal) {
        User user = principal != null ? getUserByPrincipal(principal) : null;
        TutorPostResponse response = tutorPostService.getPostById(id, user);
        return ApiResponse.success("Lấy thông tin chi tiết bài đăng thành công", response);
    }

    @PutMapping("/{id}")
    public ApiResponse<TutorPostResponse> updatePost(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTutorPostRequest request,
            Principal principal
    ) {
        User user = getUserByPrincipal(principal);
        TutorPostResponse response = tutorPostService.updatePost(id, request, user);
        return ApiResponse.success("Cập nhật bài đăng thành công", response);
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<TutorPostResponse> closePost(@PathVariable Long id, Principal principal) {
        User user = getUserByPrincipal(principal);
        TutorPostResponse response = tutorPostService.closePost(id, user);
        return ApiResponse.success("Đã đóng bài đăng tìm gia sư", response);
    }

    @GetMapping("/{id}/applications")
    public ApiResponse<List<TutorApplicationResponse>> getApplicationsForPost(
            @PathVariable Long id,
            Principal principal
    ) {
        User user = getUserByPrincipal(principal);
        List<TutorApplicationResponse> apps = tutorApplicationService.getApplicationsForPost(id, user);
        return ApiResponse.success("Lấy danh sách gia sư ứng tuyển thành công", apps);
    }

    @PostMapping("/{id}/apply")
    public ApiResponse<TutorApplicationResponse> apply(
            @PathVariable Long id,
            @Valid @RequestBody ApplyTutorPostRequest request,
            Principal principal
    ) {
        User user = getUserByPrincipal(principal);
        TutorApplicationResponse response = tutorApplicationService.apply(id, request, user);
        return ApiResponse.success("Nộp đơn ứng tuyển thành công. Vui lòng chờ phụ huynh phản hồi.", response);
    }

    @PostMapping("/{id}/applications/{appId}/accept")
    public ApiResponse<TutorApplicationResponse> acceptApplication(
            @PathVariable Long id,
            @PathVariable Long appId,
            Principal principal
    ) {
        User user = getUserByPrincipal(principal);
        TutorApplicationResponse response = tutorApplicationService.acceptApplication(id, appId, user);
        return ApiResponse.success("Bạn đã chọn gia sư thành công! Hệ thống đã tự động tạo lớp học mới.", response);
    }

    @PostMapping("/{id}/applications/{appId}/reject")
    public ApiResponse<TutorApplicationResponse> rejectApplication(
            @PathVariable Long id,
            @PathVariable Long appId,
            @RequestBody(required = false) RejectApplicationRequest request,
            Principal principal
    ) {
        User user = getUserByPrincipal(principal);
        TutorApplicationResponse response = tutorApplicationService.rejectApplication(id, appId, request, user);
        return ApiResponse.success("Đã từ chối đơn ứng tuyển của gia sư.", response);
    }

    @DeleteMapping("/applications/{appId}")
    public ApiResponse<TutorApplicationResponse> withdrawApplication(
            @PathVariable Long appId,
            Principal principal
    ) {
        User user = getUserByPrincipal(principal);
        TutorApplicationResponse response = tutorApplicationService.withdrawApplication(appId, user);
        return ApiResponse.success("Rút đơn ứng tuyển thành công", response);
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
