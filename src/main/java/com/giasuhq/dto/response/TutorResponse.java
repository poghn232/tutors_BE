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
        List<String> subjects,
        Double rating,
        Integer reviewsCount
) {
    public TutorResponse(
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
        this(
                id, fullName, qualification, email, phone, avatarUrl, facebookUrl, bio, experienceYears,
                verificationStatus, rejectionReason, verifiedAt, certificatesJson, availableDaysJson,
                availableTimesJson, createdAt, subjects, 0.0, 0
        );
    }

    public String getSubject() {
        return qualification;
    }

    public String subject() {
        return qualification;
    }
}
