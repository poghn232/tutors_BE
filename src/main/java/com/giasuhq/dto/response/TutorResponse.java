package com.giasuhq.dto.response;

import java.time.LocalDateTime;

public record TutorResponse(
        Long id,
        String fullName,
        String subject,
        String email,
        String phone,
        String avatarUrl,
        String facebookUrl,
        String bio,
        Integer experienceYears,
        String verificationStatus,
        String rejectionReason,
        LocalDateTime verifiedAt,
        String certificatesJson,
        LocalDateTime createdAt
) {
}

