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
public class ModeratePostRequest {

    @NotBlank(message = "Hành động phê duyệt không được để trống (APPROVE / REJECT)")
    private String action; // APPROVE or REJECT

    private String rejectionReason;
}
