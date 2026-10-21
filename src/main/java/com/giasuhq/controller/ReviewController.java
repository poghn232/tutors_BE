package com.giasuhq.controller;

import com.giasuhq.dto.request.ReviewRequest;
import com.giasuhq.dto.response.ApiResponse;
import com.giasuhq.dto.response.ReviewResponse;
import com.giasuhq.dto.response.TutorReviewsSummaryResponse;
import com.giasuhq.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tutors/{tutorId}/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    public ApiResponse<TutorReviewsSummaryResponse> getReviews(@PathVariable Long tutorId) {
        return ApiResponse.success("Lấy danh sách đánh giá gia sư thành công", reviewService.getReviewsForTutor(tutorId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReviewResponse> createReview(
            @PathVariable Long tutorId,
            @Valid @RequestBody ReviewRequest request,
            Authentication authentication
    ) {
        String userEmail = authentication != null ? authentication.getName() : null;
        ReviewResponse created = reviewService.createReview(tutorId, request, userEmail);
        return ApiResponse.success("Gửi đánh giá thành công!", created);
    }
}
