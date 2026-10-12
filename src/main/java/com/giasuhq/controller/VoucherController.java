package com.giasuhq.controller;

import com.giasuhq.dto.request.ClaimVoucherRequest;
import com.giasuhq.dto.request.IssueVoucherRequest;
import com.giasuhq.dto.request.VoucherQuoteRequest;
import com.giasuhq.dto.response.ApiResponse;
import com.giasuhq.dto.response.VoucherQuoteResponse;
import com.giasuhq.dto.response.VoucherResponse;
import com.giasuhq.dto.response.VoucherShareResponse;
import com.giasuhq.entity.User;
import com.giasuhq.repository.UserRepository;
import com.giasuhq.service.VoucherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;
    private final UserRepository userRepository;

    @GetMapping
    public ApiResponse<List<VoucherResponse>> getMyVouchers(Principal principal) {
        User user = getUserByPrincipal(principal);
        return ApiResponse.success("Lấy danh sách voucher thành công", voucherService.getVouchersForUser(user));
    }

    @PostMapping("/claim")
    public ApiResponse<VoucherResponse> claimVoucher(
            @Valid @RequestBody ClaimVoucherRequest request,
            Principal principal
    ) {
        User user = getUserByPrincipal(principal);
        return ApiResponse.success("Nhận voucher thành công", voucherService.claimVoucher(user, request.getCode()));
    }

    @PostMapping("/quote")
    public ApiResponse<VoucherQuoteResponse> quoteVoucher(
            @Valid @RequestBody VoucherQuoteRequest request,
            Principal principal
    ) {
        User user = getUserByPrincipal(principal);
        return ApiResponse.success("Kiểm tra voucher thành công", voucherService.quote(user, request.getCode(), request.getAmount()));
    }

    @PostMapping("/{userVoucherId}/share")
    public ApiResponse<VoucherShareResponse> shareVoucher(
            @PathVariable Long userVoucherId,
            Principal principal
    ) {
        User user = getUserByPrincipal(principal);
        return ApiResponse.success("Tạo link chia sẻ voucher thành công", voucherService.shareVoucher(user, userVoucherId));
    }

    @PostMapping("/admin/issue")
    public ApiResponse<VoucherResponse> issueVoucher(
            @Valid @RequestBody IssueVoucherRequest request,
            Principal principal
    ) {
        User admin = getUserByPrincipal(principal);
        return ApiResponse.success("Phát voucher cho người dùng thành công", voucherService.issueVoucherToUser(admin, request.getUserId(), request.getCode()));
    }

    private User getUserByPrincipal(Principal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("Vui lòng đăng nhập để sử dụng voucher.");
        }
        return userRepository.findByEmailIgnoreCase(principal.getName())
                .or(() -> userRepository.findByEmail(principal.getName()))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng hiện tại."));
    }
}
