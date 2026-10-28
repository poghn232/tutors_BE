package com.giasuhq.controller;

import com.giasuhq.dto.request.ModeratePostRequest;
import com.giasuhq.dto.response.ApiResponse;
import com.giasuhq.dto.response.TutorPostResponse;
import com.giasuhq.entity.User;
import com.giasuhq.repository.UserRepository;
import com.giasuhq.service.TutorPostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/admin/tutor-posts")
@RequiredArgsConstructor
public class AdminTutorPostController {

    private final TutorPostService tutorPostService;
    private final UserRepository userRepository;

    @GetMapping
    public ApiResponse<Page<TutorPostResponse>> getAllPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Principal principal
    ) {
        User user = getUserByPrincipal(principal);
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(size, 100));
        Page<TutorPostResponse> posts = tutorPostService.getAllPostsForAdmin(pageable);
        return ApiResponse.success("Lấy toàn bộ danh sách bài đăng thành công", posts);
    }

    @PatchMapping("/{id}/moderate")
    public ApiResponse<TutorPostResponse> moderatePost(
            @PathVariable Long id,
            @Valid @RequestBody ModeratePostRequest request,
            Principal principal
    ) {
        User user = getUserByPrincipal(principal);
        TutorPostResponse response = tutorPostService.moderatePost(id, request, user);
        String msg = "APPROVE".equalsIgnoreCase(request.getAction())
                ? "Đã phê duyệt bài đăng thành công. Bài đăng đã hiển thị công khai."
                : "Đã từ chối bài đăng thành công.";
        return ApiResponse.success(msg, response);
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
