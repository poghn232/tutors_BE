package com.giasuhq.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

public record CreateTutorRequest(
        @NotBlank String fullName,
        @NotBlank String subject,
        @Email String email,
        @Pattern(regexp = "^$|0[35789]\\d{8}", message = "Số điện thoại phải là số Việt Nam 10 chữ số") String phone,
        String facebookUrl,
        @PositiveOrZero double hourlyRate
) {
}
