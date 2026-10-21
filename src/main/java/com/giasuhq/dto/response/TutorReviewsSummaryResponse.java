package com.giasuhq.dto.response;

import java.util.List;

public record TutorReviewsSummaryResponse(
        Long tutorId,
        Double averageRating,
        Integer reviewsCount,
        List<ReviewResponse> reviews
) {}
