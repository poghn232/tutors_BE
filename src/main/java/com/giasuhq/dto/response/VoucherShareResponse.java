package com.giasuhq.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoucherShareResponse {
    private Long userVoucherId;
    private String voucherCode;
    private String shareCode;
    private String shareUrl;
    private LocalDateTime expiresAt;
}
