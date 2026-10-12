package com.giasuhq.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueVoucherRequest {
    @NotNull(message = "Thiếu ID người dùng nhận voucher")
    private Long userId;

    @NotBlank(message = "Mã voucher không được để trống")
    private String code;
}
