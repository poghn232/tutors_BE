package com.giasuhq.service.impl;

import com.giasuhq.dto.response.VoucherQuoteResponse;
import com.giasuhq.dto.response.VoucherRedemptionResult;
import com.giasuhq.dto.response.VoucherResponse;
import com.giasuhq.dto.response.VoucherShareResponse;
import com.giasuhq.entity.*;
import com.giasuhq.exception.ResourceNotFoundException;
import com.giasuhq.repository.TutoringClassRepository;
import com.giasuhq.repository.UserRepository;
import com.giasuhq.repository.UserVoucherRepository;
import com.giasuhq.repository.VoucherRedemptionRepository;
import com.giasuhq.repository.VoucherRepository;
import com.giasuhq.service.VoucherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoucherServiceImpl implements VoucherService {

    private final VoucherRepository voucherRepository;
    private final UserVoucherRepository userVoucherRepository;
    private final VoucherRedemptionRepository voucherRedemptionRepository;
    private final UserRepository userRepository;
    private final TutoringClassRepository tutoringClassRepository;

    @Override
    @Transactional
    public List<VoucherResponse> getVouchersForUser(User user) {
        List<UserVoucher> userVouchers = userVoucherRepository.findByUserIdOrderByClaimedAtDesc(user.getId());
        LocalDateTime now = LocalDateTime.now();
        boolean changed = false;

        for (UserVoucher userVoucher : userVouchers) {
            if (userVoucher.getStatus() == UserVoucherStatus.AVAILABLE
                    && isExpired(userVoucher.getVoucher(), now)) {
                userVoucher.setStatus(UserVoucherStatus.EXPIRED);
                changed = true;
            }
        }
        if (changed) {
            userVoucherRepository.saveAll(userVouchers);
        }

        return userVouchers.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public VoucherResponse claimVoucher(User user, String code) {
        requireUser(user);
        String normalizedCode = normalizeCode(code);

        UserVoucher sharedSource = userVoucherRepository.findByShareCodeIgnoreCase(normalizedCode).orElse(null);
        if (sharedSource != null) {
            return claimSharedVoucher(user, sharedSource);
        }

        Voucher voucher = voucherRepository.findByCodeIgnoreCase(normalizedCode)
                .orElseThrow(() -> new IllegalArgumentException("Mã voucher không tồn tại hoặc đã bị khóa."));
        ensureVoucherClaimable(voucher);

        UserVoucher existing = userVoucherRepository.findByUserIdAndVoucherId(user.getId(), voucher.getId()).orElse(null);
        if (existing != null) {
            if (existing.getStatus() == UserVoucherStatus.REDEEMED) {
                throw new IllegalArgumentException("Voucher này đã được sử dụng trước đó.");
            }
            if (existing.getStatus() == UserVoucherStatus.EXPIRED) {
                throw new IllegalArgumentException("Voucher này đã hết hạn.");
            }
            return mapToResponse(existing);
        }

        UserVoucher claimed = createUserVoucher(voucher, user, null);
        incrementClaimCount(voucher);
        return mapToResponse(userVoucherRepository.save(claimed));
    }

    @Override
    @Transactional
    public VoucherShareResponse shareVoucher(User user, Long userVoucherId) {
        requireUser(user);
        UserVoucher userVoucher = userVoucherRepository.findById(userVoucherId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy voucher của người dùng."));

        if (userVoucher.getUser() == null || !user.getId().equals(userVoucher.getUser().getId())) {
            throw new IllegalArgumentException("Bạn không có quyền chia sẻ voucher này.");
        }
        if (userVoucher.getStatus() != UserVoucherStatus.AVAILABLE) {
            throw new IllegalArgumentException("Chỉ voucher còn hiệu lực mới có thể chia sẻ.");
        }
        Voucher voucher = userVoucher.getVoucher();
        ensureVoucherValid(voucher);
        if (!Boolean.TRUE.equals(voucher.getShareable())) {
            throw new IllegalArgumentException("Voucher này không hỗ trợ chia sẻ.");
        }

        if (userVoucher.getShareCode() == null || userVoucher.getShareCode().isBlank()) {
            userVoucher.setShareCode(generateShareCode());
            userVoucher.setSharedAt(LocalDateTime.now());
            userVoucherRepository.save(userVoucher);
        }

        return VoucherShareResponse.builder()
                .userVoucherId(userVoucher.getId())
                .voucherCode(voucher.getCode())
                .shareCode(userVoucher.getShareCode())
                .shareUrl("/#promotions?claim=" + userVoucher.getShareCode())
                .expiresAt(voucher.getExpiresAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public VoucherQuoteResponse quote(User user, String code, BigDecimal amount) {
        requireUser(user);
        BigDecimal originalAmount = normalizeAmount(amount);
        UserVoucher userVoucher = findOwnedVoucher(user, code);
        ensureUsable(userVoucher);

        BigDecimal discount = calculateDiscount(userVoucher.getVoucher(), originalAmount);
        return VoucherQuoteResponse.builder()
                .valid(true)
                .code(userVoucher.getVoucher().getCode())
                .originalAmount(originalAmount)
                .discountAmount(discount)
                .finalAmount(originalAmount.subtract(discount).max(BigDecimal.ZERO))
                .message("Voucher hợp lệ.")
                .build();
    }

    @Override
    @Transactional
    public VoucherRedemptionResult redeem(User user, String code, BigDecimal amount, Long classId) {
        requireUser(user);
        BigDecimal originalAmount = normalizeAmount(amount);
        UserVoucher userVoucher = findOwnedVoucher(user, code);
        ensureUsable(userVoucher);

        Voucher voucher = userVoucher.getVoucher();
        BigDecimal discount = calculateDiscount(voucher, originalAmount);
        BigDecimal finalAmount = originalAmount.subtract(discount).max(BigDecimal.ZERO);

        userVoucher.setStatus(UserVoucherStatus.REDEEMED);
        userVoucher.setRedeemedAt(LocalDateTime.now());
        userVoucherRepository.save(userVoucher);

        VoucherRedemption redemption = VoucherRedemption.builder()
                .voucher(voucher)
                .user(user)
                .userVoucher(userVoucher)
                .tutoringClass(classId != null ? tutoringClassRepository.findById(classId).orElse(null) : null)
                .originalAmount(originalAmount)
                .discountAmount(discount)
                .finalAmount(finalAmount)
                .redeemedAt(LocalDateTime.now())
                .build();
        voucherRedemptionRepository.save(redemption);

        return VoucherRedemptionResult.builder()
                .code(voucher.getCode())
                .originalAmount(originalAmount)
                .discountAmount(discount)
                .finalAmount(finalAmount)
                .voucher(mapToResponse(userVoucher))
                .build();
    }

    @Override
    @Transactional
    public VoucherResponse issuePostPaymentVoucher(User user) {
        requireUser(user);
        LocalDateTime now = LocalDateTime.now();
        List<Voucher> candidates = voucherRepository.findByActiveTrueOrderByCreatedAtAsc();

        for (Voucher voucher : candidates) {
            if (!Boolean.TRUE.equals(voucher.getAutoIssueOnPayment()) || !isValidAt(voucher, now)) {
                continue;
            }
            if (userVoucherRepository.findByUserIdAndVoucherId(user.getId(), voucher.getId()).isPresent()) {
                continue;
            }
            if (isUsageLimitReached(voucher)) {
                continue;
            }

            UserVoucher issued = createUserVoucher(voucher, user, null);
            incrementClaimCount(voucher);
            return mapToResponse(userVoucherRepository.save(issued));
        }

        return null;
    }

    @Override
    @Transactional
    public VoucherResponse issueVoucherToUser(User actor, Long targetUserId, String code) {
        requireUser(actor);
        if (actor.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("Chỉ quản trị viên mới có quyền phát voucher trực tiếp.");
        }
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng nhận voucher."));
        return claimVoucher(target, code);
    }

    private VoucherResponse claimSharedVoucher(User receiver, UserVoucher source) {
        Voucher voucher = source.getVoucher();
        ensureVoucherClaimable(voucher);
        if (source.getUser() != null && receiver.getId().equals(source.getUser().getId())) {
            throw new IllegalArgumentException("Bạn không thể tự nhận voucher do chính mình chia sẻ.");
        }
        if (source.getSharedClaimedAt() != null) {
            throw new IllegalArgumentException("Link chia sẻ voucher này đã được sử dụng.");
        }
        if (userVoucherRepository.findByUserIdAndVoucherId(receiver.getId(), voucher.getId()).isPresent()) {
            throw new IllegalArgumentException("Tài khoản của bạn đã có voucher này.");
        }

        UserVoucher claimed = createUserVoucher(voucher, receiver, source);
        source.setSharedClaimedAt(LocalDateTime.now());
        userVoucherRepository.save(source);
        incrementClaimCount(voucher);
        return mapToResponse(userVoucherRepository.save(claimed));
    }

    private UserVoucher findOwnedVoucher(User user, String code) {
        String normalizedCode = normalizeCode(code);
        Voucher voucher = voucherRepository.findByCodeIgnoreCase(normalizedCode)
                .orElseThrow(() -> new IllegalArgumentException("Bạn chưa sở hữu voucher này hoặc mã không tồn tại."));
        return userVoucherRepository.findByUserIdAndVoucherId(user.getId(), voucher.getId())
                .orElseThrow(() -> new IllegalArgumentException("Hãy nhận voucher trước khi áp dụng vào thanh toán."));
    }

    private UserVoucher createUserVoucher(Voucher voucher, User user, UserVoucher source) {
        return UserVoucher.builder()
                .voucher(voucher)
                .user(user)
                .status(UserVoucherStatus.AVAILABLE)
                .sourceUserVoucher(source)
                .claimedAt(LocalDateTime.now())
                .build();
    }

    private void ensureUsable(UserVoucher userVoucher) {
        if (userVoucher.getStatus() != UserVoucherStatus.AVAILABLE) {
            throw new IllegalArgumentException("Voucher đã được sử dụng hoặc không còn hiệu lực.");
        }
        ensureVoucherValid(userVoucher.getVoucher());
    }

    private void ensureVoucherClaimable(Voucher voucher) {
        ensureVoucherValid(voucher);
        if (isUsageLimitReached(voucher)) {
            throw new IllegalArgumentException("Voucher đã hết lượt phát.");
        }
    }

    private void ensureVoucherValid(Voucher voucher) {
        if (voucher == null || !isValidAt(voucher, LocalDateTime.now())) {
            throw new IllegalArgumentException("Voucher chưa bắt đầu, đã hết hạn hoặc đã bị khóa.");
        }
    }

    private boolean isValidAt(Voucher voucher, LocalDateTime now) {
        return voucher != null
                && Boolean.TRUE.equals(voucher.getActive())
                && (voucher.getStartsAt() == null || !now.isBefore(voucher.getStartsAt()))
                && (voucher.getExpiresAt() == null || now.isBefore(voucher.getExpiresAt()));
    }

    private boolean isExpired(Voucher voucher, LocalDateTime now) {
        return voucher != null && voucher.getExpiresAt() != null && !now.isBefore(voucher.getExpiresAt());
    }

    private boolean isUsageLimitReached(Voucher voucher) {
        return voucher.getUsageLimit() != null
                && voucher.getUsageLimit() > 0
                && voucher.getClaimedCount() != null
                && voucher.getClaimedCount() >= voucher.getUsageLimit();
    }

    private void incrementClaimCount(Voucher voucher) {
        voucher.setClaimedCount((voucher.getClaimedCount() == null ? 0 : voucher.getClaimedCount()) + 1);
        voucherRepository.save(voucher);
    }

    private BigDecimal calculateDiscount(Voucher voucher, BigDecimal amount) {
        if (amount.compareTo(defaultZero(voucher.getMinOrderAmount())) < 0) {
            throw new IllegalArgumentException("Đơn hàng chưa đạt giá trị tối thiểu để dùng voucher.");
        }

        BigDecimal discount;
        if (voucher.getDiscountType() == VoucherDiscountType.PERCENTAGE) {
            discount = amount.multiply(voucher.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        } else {
            discount = voucher.getDiscountValue();
        }

        if (voucher.getMaxDiscount() != null) {
            discount = discount.min(voucher.getMaxDiscount());
        }
        return discount.max(BigDecimal.ZERO).min(amount);
    }

    private VoucherResponse mapToResponse(UserVoucher userVoucher) {
        Voucher voucher = userVoucher.getVoucher();
        return VoucherResponse.builder()
                .id(userVoucher.getId())
                .code(voucher.getCode())
                .name(voucher.getName())
                .description(voucher.getDescription())
                .discountType(voucher.getDiscountType())
                .discountValue(voucher.getDiscountValue())
                .maxDiscount(voucher.getMaxDiscount())
                .minOrderAmount(voucher.getMinOrderAmount())
                .startsAt(voucher.getStartsAt())
                .expiresAt(voucher.getExpiresAt())
                .shareable(voucher.getShareable())
                .shareCode(userVoucher.getShareCode())
                .status(userVoucher.getStatus())
                .claimedAt(userVoucher.getClaimedAt())
                .redeemedAt(userVoucher.getRedeemedAt())
                .build();
    }

    private String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Mã voucher không được để trống.");
        }
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Giá trị thanh toán phải lớn hơn 0.");
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String generateShareCode() {
        return "SHR-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
    }

    private void requireUser(User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("Vui lòng đăng nhập để sử dụng voucher.");
        }
    }
}
