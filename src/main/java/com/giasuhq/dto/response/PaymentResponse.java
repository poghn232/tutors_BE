package com.giasuhq.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private String status;
    private String gatewayNotice;
    private Double totalPendingFee;
    private Double totalPaidFee;
    private List<InvoiceItem> invoices;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvoiceItem {
        private Long id;
        private String className;
        private String period;
        private Double amount;
        private String status; // PENDING, PAID
        private String dueDate;
    }
}
