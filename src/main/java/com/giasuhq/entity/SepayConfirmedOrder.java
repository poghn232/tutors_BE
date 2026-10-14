package com.giasuhq.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "sepay_confirmed_orders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SepayConfirmedOrder {

    @Id
    @Column(name = "order_code", length = 80, nullable = false)
    private String orderCode;

    @Column(name = "webhook_id")
    private Long webhookId;

    @Column(length = 80)
    private String gateway;

    @Column(name = "transaction_date", length = 80)
    private String transactionDate;

    @Column(name = "account_number", length = 80)
    private String accountNumber;

    @Column(name = "transfer_amount")
    private Long transferAmount;

    @Column(name = "reference_code", length = 160)
    private String referenceCode;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "confirmed_at", nullable = false)
    private LocalDateTime confirmedAt;

    @PrePersist
    @PreUpdate
    protected void updateTimestamp() {
        confirmedAt = LocalDateTime.now();
    }
}
