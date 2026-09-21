package com.giasuhq.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record CreateTutorRequest(
        @NotBlank String fullName,
        @NotBlank String subject,
        @Email String email,
        String phone,
        String facebookUrl,
        @PositiveOrZero double hourlyRate
) {
}
