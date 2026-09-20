package com.giasuhq.dto.request;

import com.giasuhq.entity.Role;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleLoginRequest {

    @NotBlank(message = "Google ID Token không được để trống")
    private String idToken;

    private Role role;
}
