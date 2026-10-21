package com.giasuhq.dto.response;

import java.time.LocalDateTime;

public record ReviewResponse(
        Long id,
        Long tutorId,
        Long userId,
        String authorName,
        Integer rating,
        String comment,
        LocalDateTime createdAt,
        String dateFormatted
) {}
