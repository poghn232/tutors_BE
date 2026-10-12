package com.giasuhq.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_vouchers", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_voucher_owner", columnNames = {"voucher_id", "user_id"}),
        @UniqueConstraint(name = "uk_user_voucher_share_code", columnNames = {"share_code"})
}, indexes = {
        @Index(name = "idx_user_vouchers_user_status", columnList = "user_id, status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserVoucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "voucher_id", nullable = false)
    private Voucher voucher;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private UserVoucherStatus status = UserVoucherStatus.AVAILABLE;

    @Column(name = "share_code", length = 80)
    private String shareCode;

    @Column(name = "claimed_at", nullable = false)
    private LocalDateTime claimedAt;

    @Column(name = "shared_at")
    private LocalDateTime sharedAt;

    @Column(name = "shared_claimed_at")
    private LocalDateTime sharedClaimedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_user_voucher_id")
    private UserVoucher sourceUserVoucher;

    @Column(name = "redeemed_at")
    private LocalDateTime redeemedAt;

    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        if (this.claimedAt == null) {
            this.claimedAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = UserVoucherStatus.AVAILABLE;
        }
    }
}
