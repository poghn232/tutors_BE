package com.giasuhq.controller;

import com.giasuhq.dto.request.CreateSubjectRequest;
import com.giasuhq.dto.response.ApiResponse;
import com.giasuhq.dto.response.SubjectResponse;
import com.giasuhq.service.SubjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subjects")
@RequiredArgsConstructor
public class SubjectController {

    private final SubjectService subjectService;

    @GetMapping
    public ApiResponse<List<SubjectResponse>> getAllSubjects() {
        List<SubjectResponse> subjects = subjectService.getAllSubjects();
        return ApiResponse.success("Lấy danh sách môn học thành công", subjects);
    }

    @PostMapping
    public ApiResponse<SubjectResponse> createSubject(@Valid @RequestBody CreateSubjectRequest request) {
        SubjectResponse response = subjectService.createSubject(request);
        return ApiResponse.success("Thêm môn học mới thành công", response);
    }
}
