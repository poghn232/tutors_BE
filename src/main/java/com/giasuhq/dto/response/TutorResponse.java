package com.giasuhq.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record TutorResponse(
        Long id,
        String fullName,
        String qualification,
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
        String availableDaysJson,
        String availableTimesJson,
        LocalDateTime createdAt,
        List<String> subjects
) {
    public String getSubject() {
        return qualification;
    }

    public String subject() {
        return qualification;
    }
}

