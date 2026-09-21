package com.giasuhq.dto.response;

public record TutorResponse(
        Long id,
        String fullName,
        String subject,
        String email,
        String phone,
        String facebookUrl,
        String bio,
        Integer experienceYears
) {
}
