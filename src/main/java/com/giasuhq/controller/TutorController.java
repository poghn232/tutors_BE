package com.giasuhq.controller;

import com.giasuhq.dto.request.CreateTutorRequest;
import com.giasuhq.dto.response.ApiResponse;
import com.giasuhq.dto.response.TutorResponse;
import com.giasuhq.service.TutorService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tutors")
public class TutorController {

    private final TutorService tutorService;

    public TutorController(TutorService tutorService) {
        this.tutorService = tutorService;
    }

    @GetMapping
    public ApiResponse<List<TutorResponse>> getTutors() {
        return ApiResponse.success(tutorService.findAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<TutorResponse> getTutor(@PathVariable Long id) {
        return ApiResponse.success(tutorService.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TutorResponse> createTutor(@Valid @RequestBody CreateTutorRequest request) {
        return ApiResponse.success(tutorService.create(request));
    }
}
