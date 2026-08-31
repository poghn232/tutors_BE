package com.giasuhq.controller;

import com.giasuhq.dto.request.CreateLessonNoteRequest;
import com.giasuhq.dto.request.CreateLessonRequest;
import com.giasuhq.dto.request.UpdateLessonStatusRequest;
import com.giasuhq.dto.response.ApiResponse;
import com.giasuhq.dto.response.LessonNoteResponse;
import com.giasuhq.dto.response.LessonResponse;
import com.giasuhq.entity.User;
import com.giasuhq.repository.UserRepository;
import com.giasuhq.service.LessonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/lessons")
@RequiredArgsConstructor
public class LessonController {

    private final LessonService lessonService;
    private final UserRepository userRepository;

    @GetMapping
    public ApiResponse<List<LessonResponse>> getLessons(Principal principal) {
        User user = getUserByPrincipal(principal);
        List<LessonResponse> lessons = lessonService.getLessonsForUser(user);
        return ApiResponse.success("Lấy danh sách buổi học thành công", lessons);
    }

    @PostMapping
    public ApiResponse<LessonResponse> createLesson(@Valid @RequestBody CreateLessonRequest request, Principal principal) {
        User user = getUserByPrincipal(principal);
        LessonResponse response = lessonService.createLesson(request, user);
        return ApiResponse.success("Tạo buổi học mới thành công!", response);
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<LessonResponse> updateLessonStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateLessonStatusRequest request,
            Principal principal) {
        User user = getUserByPrincipal(principal);
        LessonResponse response = lessonService.updateLessonStatus(id, request.getStatus(), user);
        return ApiResponse.success("Cập nhật trạng thái buổi học thành công!", response);
    }

    @PostMapping("/{id}/note")
    public ApiResponse<LessonNoteResponse> addOrUpdateLessonNote(
            @PathVariable Long id,
            @Valid @RequestBody CreateLessonNoteRequest request,
            Principal principal) {
        User user = getUserByPrincipal(principal);
        LessonNoteResponse response = lessonService.addOrUpdateLessonNote(id, request, user);
        return ApiResponse.success("Lưu ghi chú buổi học & AI Note thành công!", response);
    }

    private User getUserByPrincipal(Principal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("Người dùng chưa đăng nhập hoặc phiên làm việc hết hạn.");
        }
        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng với email: " + principal.getName()));
    }
}
