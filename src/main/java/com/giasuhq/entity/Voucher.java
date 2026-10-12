package com.giasuhq.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "vouchers", indexes = {
        @Index(name = "idx_vouchers_code", columnList = "code"),
        @Index(name = "idx_vouchers_active_window", columnList = "active, starts_at, expires_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private VoucherDiscountType discountType;

    @Column(name = "discount_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "max_discount", precision = 15, scale = 2)
    private BigDecimal maxDiscount;

    @Column(name = "min_order_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal minOrderAmount = BigDecimal.ZERO;

    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Column(name = "claimed_count", nullable = false)
    @Builder.Default
    private Integer claimedCount = 0;

    @Column(name = "max_claims_per_user", nullable = false)
    @Builder.Default
    private Integer maxClaimsPerUser = 1;

    @Column(name = "starts_at")
    private LocalDateTime startsAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "shareable", nullable = false)
    @Builder.Default
    private Boolean shareable = false;

    @Column(name = "auto_issue_on_payment", nullable = false)
    @Builder.Default
    private Boolean autoIssueOnPayment = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        normalizeDefaults();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        normalizeDefaults();
    }

    private void normalizeDefaults() {
        if (this.minOrderAmount == null) {
            this.minOrderAmount = BigDecimal.ZERO;
        }
        if (this.claimedCount == null) {
            this.claimedCount = 0;
        }
        if (this.maxClaimsPerUser == null || this.maxClaimsPerUser < 1) {
            this.maxClaimsPerUser = 1;
        }
        if (this.active == null) {
            this.active = true;
        }
        if (this.shareable == null) {
            this.shareable = false;
        }
        if (this.autoIssueOnPayment == null) {
            this.autoIssueOnPayment = false;
        }
    }
}
