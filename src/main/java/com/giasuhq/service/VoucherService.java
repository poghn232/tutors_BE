package com.giasuhq.service;

import com.giasuhq.dto.response.VoucherQuoteResponse;
import com.giasuhq.dto.response.VoucherResponse;
import com.giasuhq.dto.response.VoucherShareResponse;
import com.giasuhq.dto.response.VoucherRedemptionResult;
import com.giasuhq.entity.User;

import java.math.BigDecimal;
import java.util.List;

public interface VoucherService {
    List<VoucherResponse> getVouchersForUser(User user);

    VoucherResponse claimVoucher(User user, String code);

    VoucherShareResponse shareVoucher(User user, Long userVoucherId);

    VoucherQuoteResponse quote(User user, String code, BigDecimal amount);

    VoucherRedemptionResult redeem(User user, String code, BigDecimal amount, Long classId);

    VoucherResponse issuePostPaymentVoucher(User user);

    VoucherResponse issueVoucherToUser(User actor, Long targetUserId, String code);
}
