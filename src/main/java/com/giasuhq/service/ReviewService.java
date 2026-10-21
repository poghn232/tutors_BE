package com.giasuhq.service;

import com.giasuhq.dto.request.ReviewRequest;
import com.giasuhq.dto.response.ReviewResponse;
import com.giasuhq.dto.response.TutorReviewsSummaryResponse;

public interface ReviewService {
    TutorReviewsSummaryResponse getReviewsForTutor(Long tutorId);
    ReviewResponse createReview(Long tutorId, ReviewRequest request, String currentUserEmail);
    Double getAverageRating(Long tutorId);
    int getReviewsCount(Long tutorId);
}
