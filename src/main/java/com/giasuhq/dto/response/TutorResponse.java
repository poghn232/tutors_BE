package com.giasuhq.dto.response;

public record TutorResponse(
        Long id,
        String fullName,
        String subject,
        String email,
        double hourlyRate
) {
}
