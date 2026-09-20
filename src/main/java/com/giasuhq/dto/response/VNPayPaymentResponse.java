package com.giasuhq.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VNPayPaymentResponse {
    private String paymentUrl;
    private String txnRef;
    private long amount;
    private String orderInfo;
    private String responseCode;
    private String transactionNo;
    private String bankCode;
    private String payDate;
    private String message;
    private boolean success;
}
