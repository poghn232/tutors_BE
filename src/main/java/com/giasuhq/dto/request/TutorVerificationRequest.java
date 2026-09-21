package com.giasuhq.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TutorVerificationRequest {

    @NotBlank(message = "Trạng thái xác thực không được để trống")
    private String status; // APPROVED, REJECTED, PENDING

    private String reason;
}
