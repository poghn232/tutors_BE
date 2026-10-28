package com.giasuhq.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplyTutorPostRequest {

    @NotBlank(message = "Lời chào và giới thiệu năng lực không được để trống")
    private String coverLetter;

    private BigDecimal proposedRate;
}
