package com.giasuhq.controller;

import com.giasuhq.config.VNPayConfig;
import com.giasuhq.dto.request.SepayWebhookRequest;
import com.giasuhq.dto.request.VNPayPaymentRequest;
import com.giasuhq.dto.response.ApiResponse;
import com.giasuhq.dto.response.PaymentResponse;
import com.giasuhq.dto.response.VNPayPaymentResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import com.giasuhq.entity.User;
import com.giasuhq.repository.UserRepository;
import com.giasuhq.service.UserService;
import java.security.Principal;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final VNPayConfig vnPayConfig;
    private final UserRepository userRepository;
    private final UserService userService;
    private static final Map<String, SepayWebhookRequest> confirmedSepayOrders = new ConcurrentHashMap<>();

    @GetMapping
    public ApiResponse<PaymentResponse> getPaymentOverview() {
        PaymentResponse response = PaymentResponse.builder()
                .status("ACTIVE")
                .gatewayNotice("Cổng thanh toán VNPay Sandbox đã kết nối thành công. Học viên có thể thanh toán học phí và nâng cấp VIP trực tuyến.")
                .totalPendingFee(1200000.0)
                .totalPaidFee(2400000.0)
                .invoices(Arrays.asList(
                        PaymentResponse.InvoiceItem.builder()
                                .id(101L)
                                .className("Lớp Toán 12 - Ôn thi ĐHQG")
                                .period("Tháng 09/2026 (4 buổi)")
                                .amount(1200000.0)
                                .status("PENDING")
                                .dueDate("15/09/2026")
                                .build(),
                        PaymentResponse.InvoiceItem.builder()
                                .id(100L)
                                .className("Lớp Tiếng Anh 12 - IELTS 7.0")
                                .period("Tháng 08/2026 (8 buổi)")
                                .amount(2400000.0)
                                .status("PAID")
                                .dueDate("01/09/2026")
                                .build()
                ))
                .build();

        return ApiResponse.success("Lấy thông tin học phí thành công", response);
    }

    /**
     * Create VNPay Payment URL
     */
    @PostMapping("/vnpay/create-payment")
    public ApiResponse<VNPayPaymentResponse> createVNPayPayment(
            @RequestBody VNPayPaymentRequest request,
            HttpServletRequest httpServletRequest
    ) {
        long amount = request.getAmount();
        if (amount <= 0) {
            amount = 199000; // Default amount if not specified
        }

        String orderInfo = request.getOrderInfo();
        if (orderInfo == null || orderInfo.trim().isEmpty()) {
            orderInfo = "Thanh toan hoc phi Tutora - Don hang #" + System.currentTimeMillis();
        }

        String vnp_Version = "2.1.0";
        String vnp_Command = "pay";
        String vnp_TxnRef = VNPayConfig.getRandomNumber(8);
        String vnp_IpAddr = VNPayConfig.getIpAddress(httpServletRequest);
        String vnp_TmnCode = vnPayConfig.getTmnCode();

        // VNPay amount is in VND multiplied by 100
        long vnpAmount = amount * 100;

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(vnpAmount));
        vnp_Params.put("vnp_CurrCode", "VND");

        if (request.getBankCode() != null && !request.getBankCode().isEmpty()) {
            vnp_Params.put("vnp_BankCode", request.getBankCode());
        }

        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", orderInfo);
        vnp_Params.put("vnp_OrderType", request.getOrderType() != null ? request.getOrderType() : "other");
        vnp_Params.put("vnp_Locale", "vn");

        String returnUrl = request.getReturnUrl();
        if (returnUrl == null || returnUrl.trim().isEmpty()) {
            returnUrl = vnPayConfig.getReturnUrl();
        }
        vnp_Params.put("vnp_ReturnUrl", returnUrl);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        formatter.setTimeZone(TimeZone.getTimeZone("Etc/GMT+7"));
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        // Build query string and hash
        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                // Build hash data
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                // Build query
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII));
                query.append('=');
                query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }

        String queryUrl = query.toString();
        String vnp_SecureHash = VNPayConfig.hmacSHA512(vnPayConfig.getHashSecret(), hashData.toString());
        String paymentUrl = vnPayConfig.getPayUrl() + "?" + queryUrl + "&vnp_SecureHash=" + vnp_SecureHash;

        VNPayPaymentResponse response = VNPayPaymentResponse.builder()
                .paymentUrl(paymentUrl)
                .txnRef(vnp_TxnRef)
                .amount(amount)
                .orderInfo(orderInfo)
                .success(true)
                .message("Tạo URL thanh toán VNPay thành công.")
                .build();

        return ApiResponse.success("Tạo URL thanh toán thành công", response);
    }

    /**
     * Handle Callback from VNPay (after user completes payment on VNPay portal)
     */
    @GetMapping("/vnpay/callback")
    public ApiResponse<VNPayPaymentResponse> vnpayCallback(@RequestParam Map<String, String> queryParams) {
        String vnp_SecureHash = queryParams.get("vnp_SecureHash");
        Map<String, String> fields = new HashMap<>(queryParams);
        fields.remove("vnp_SecureHashType");
        fields.remove("vnp_SecureHash");

        // Verify checksum
        String signValue = VNPayConfig.hashAllFields(fields, vnPayConfig.getHashSecret());
        boolean isValidSignature = signValue.equalsIgnoreCase(vnp_SecureHash);

        String responseCode = queryParams.get("vnp_ResponseCode");
        String txnRef = queryParams.get("vnp_TxnRef");
        String transactionNo = queryParams.get("vnp_TransactionNo");
        String bankCode = queryParams.get("vnp_BankCode");
        String payDate = queryParams.get("vnp_PayDate");
        String orderInfo = queryParams.get("vnp_OrderInfo");
        long amount = 0;
        try {
            amount = Long.parseLong(queryParams.get("vnp_Amount")) / 100;
        } catch (Exception ignored) {}

        boolean isSuccess = isValidSignature && "00".equals(responseCode);
        String message;

        if (!isValidSignature) {
            message = "Chữ ký số VNPay không hợp lệ (Dữ liệu có thể bị can thiệp).";
        } else if ("00".equals(responseCode)) {
            message = "Giao dịch thanh toán thành công qua VNPay!";
        } else if ("24".equals(responseCode)) {
            message = "Giao dịch đã bị khách hàng hủy.";
        } else if ("51".equals(responseCode)) {
            message = "Tài khoản của quý khách không đủ số dư để thực hiện giao dịch.";
        } else {
            message = "Giao dịch không thành công. Mã lỗi VNPay: " + responseCode;
        }

        VNPayPaymentResponse response = VNPayPaymentResponse.builder()
                .success(isSuccess)
                .txnRef(txnRef)
                .amount(amount)
                .responseCode(responseCode)
                .transactionNo(transactionNo)
                .bankCode(bankCode)
                .payDate(payDate)
                .orderInfo(orderInfo)
                .message(message)
                .build();

        return ApiResponse.success(isSuccess ? "Thanh toán thành công" : "Thanh toán thất bại", response);
    }

    /**
     * IPN Webhook for Server-to-Server asynchronous confirmation
     */
    @GetMapping("/vnpay/ipn")
    public ResponseEntity<Map<String, String>> vnpayIpn(@RequestParam Map<String, String> queryParams) {
        Map<String, String> result = new HashMap<>();
        String vnp_SecureHash = queryParams.get("vnp_SecureHash");
        Map<String, String> fields = new HashMap<>(queryParams);
        fields.remove("vnp_SecureHashType");
        fields.remove("vnp_SecureHash");

        String signValue = VNPayConfig.hashAllFields(fields, vnPayConfig.getHashSecret());
        if (!signValue.equalsIgnoreCase(vnp_SecureHash)) {
            result.put("RspCode", "97");
            result.put("Message", "Invalid Checksum");
            return ResponseEntity.ok(result);
        }

        String responseCode = queryParams.get("vnp_ResponseCode");
        if ("00".equals(responseCode)) {
            // Confirm invoice or activate VIP subscription in database
            result.put("RspCode", "00");
            result.put("Message", "Confirm Success");
        } else {
            result.put("RspCode", "01");
            result.put("Message", "Order failed or cancelled");
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Webhook endpoint called by SePay when a real bank transfer arrives
     */
    @PostMapping("/sepay/webhook")
    public ResponseEntity<Map<String, Object>> handleSepayWebhook(
            @RequestBody SepayWebhookRequest payload,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        Map<String, Object> result = new HashMap<>();
        if (payload == null) {
            result.put("success", false);
            result.put("message", "Payload is empty");
            return ResponseEntity.badRequest().body(result);
        }

        String content = payload.getContent() != null ? payload.getContent() : "";
        if (payload.getCode() != null && !payload.getCode().isEmpty()) {
            confirmedSepayOrders.put(payload.getCode().toUpperCase().trim(), payload);
        }
        if (payload.getId() != null) {
            confirmedSepayOrders.put(String.valueOf(payload.getId()), payload);
        }

        // Search for order code inside content (e.g. TUTORA1024 or VIP1024)
        Pattern pattern = Pattern.compile("(?i)(TUTORA[0-9A-Z]+|VIP[0-9A-Z]+|[0-9]{6,10})");
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            String extractedCode = matcher.group(1).toUpperCase().trim();
            confirmedSepayOrders.put(extractedCode, payload);
        }

        result.put("success", true);
        result.put("message", "Sepay webhook processed successfully");
        return ResponseEntity.ok(result);
    }

    /**
     * Polling endpoint for frontend to check if a specific SePay transaction has arrived
     */
    @GetMapping("/sepay/check-status")
    public ApiResponse<Map<String, Object>> checkSepayStatus(
            @RequestParam String orderCode,
            Principal principal
    ) {
        Map<String, Object> data = new HashMap<>();
        String key = orderCode.toUpperCase().trim();
        boolean isPaid = confirmedSepayOrders.containsKey(key);
        data.put("paid", isPaid);
        if (isPaid) {
            SepayWebhookRequest tx = confirmedSepayOrders.get(key);
            data.put("amount", tx.getTransferAmount());
            data.put("transactionDate", tx.getTransactionDate());
            data.put("referenceCode", tx.getReferenceCode());
            data.put("gateway", tx.getGateway());
            data.put("accountNumber", tx.getAccountNumber());

            // Auto-activate VIP in database if user is logged in
            if (principal != null) {
                try {
                    String email = principal.getName();
                    userRepository.findByEmailIgnoreCase(email)
                            .or(() -> userRepository.findByEmail(email))
                            .ifPresent(u -> {
                                u.setIsVip(true);
                                userRepository.save(u);
                                data.put("vipActivated", true);
                            });
                } catch (Exception ignored) {}
            }
        }
        return ApiResponse.success("Kiểm tra trạng thái SePay", data);
    }

    /**
     * Explicit VIP Activation endpoint for logged in users
     */
    @PostMapping("/activate-vip")
    public ApiResponse<Map<String, Object>> activateVip(Principal principal) {
        if (principal == null) {
            return ApiResponse.error("Vui lòng đăng nhập để kích hoạt quyền lợi VIP.");
        }
        String email = principal.getName();
        User user = userRepository.findByEmailIgnoreCase(email)
                .or(() -> userRepository.findByEmail(email))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng."));
        user.setIsVip(true);
        userRepository.save(user);

        Map<String, Object> data = new HashMap<>();
        data.put("isVip", true);
        data.put("email", user.getEmail());
        data.put("fullName", user.getFullName());
        return ApiResponse.success("Đặc quyền VIP đã được kích hoạt thành công vào cơ sở dữ liệu!", data);
    }
}
