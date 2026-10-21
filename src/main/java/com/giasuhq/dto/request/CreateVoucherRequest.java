package com.giasuhq.dto.request;

import com.giasuhq.entity.VoucherDiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CreateVoucherRequest {

    @NotBlank(message = "Mã voucher không được để trống")
    private String code;

    @NotBlank(message = "Tên chương trình không được để trống")
    private String name;

    private String description;

    @NotNull(message = "Loại giảm giá không được để trống")
    private VoucherDiscountType discountType;

    @NotNull(message = "Giá trị giảm không được để trống")
    @DecimalMin(value = "0.01", message = "Giá trị giảm phải lớn hơn 0")
    private BigDecimal discountValue;

    private BigDecimal maxDiscount;

    private BigDecimal minOrderAmount;

    private Integer usageLimit;

    private Integer maxClaimsPerUser;

    private LocalDateTime startsAt;

    private LocalDateTime expiresAt;

    private Boolean active;

    private Boolean shareable;

    private Boolean autoIssueOnPayment;
}
