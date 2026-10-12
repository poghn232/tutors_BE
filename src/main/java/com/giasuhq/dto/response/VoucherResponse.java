package com.giasuhq.dto.response;

import com.giasuhq.entity.UserVoucherStatus;
import com.giasuhq.entity.VoucherDiscountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoucherResponse {
    private Long id;
    private String code;
    private String name;
    private String description;
    private VoucherDiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal maxDiscount;
    private BigDecimal minOrderAmount;
    private LocalDateTime startsAt;
    private LocalDateTime expiresAt;
    private Boolean shareable;
    private String shareCode;
    private UserVoucherStatus status;
    private LocalDateTime claimedAt;
    private LocalDateTime redeemedAt;
}
