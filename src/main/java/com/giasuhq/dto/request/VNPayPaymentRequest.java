package com.giasuhq.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VNPayPaymentRequest {
    private long amount;
    private String orderInfo;
    private String orderType;
    private String bankCode;
    private Long invoiceId;
    private String planId;
    private String returnUrl;
}
