package com.giasuhq.controller;

import com.giasuhq.config.VNPayConfig;
import com.giasuhq.dto.request.CreateClassRequest;
import com.giasuhq.dto.request.SepayWebhookRequest;
import com.giasuhq.dto.request.VNPayPaymentRequest;
import com.giasuhq.dto.response.ApiResponse;
import com.giasuhq.dto.response.ClassResponse;
import com.giasuhq.dto.response.PaymentResponse;
import com.giasuhq.dto.response.VNPayPaymentResponse;
import com.giasuhq.service.TutoringClassService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
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
    private final TutoringClassService tutoringClassService;
    private static final Map<String, SepayWebhookRequest> confirmedSepayOrders = new ConcurrentHashMap<>();
    private static final Map<String, CreateClassRequest> pendingBookingRequests = new ConcurrentHashMap<>();

    @GetMapping
    public ApiResponse<PaymentResponse> getPaymentOverview(Principal principal) {
        BigDecimal balance = BigDecimal.ZERO;
        if (principal != null) {
            balance = userRepository.findByEmailIgnoreCase(principal.getName())
                    .or(() -> userRepository.findByEmail(principal.getName()))
                    .map(User::getBalance)
                    .orElse(BigDecimal.ZERO);
        }
        PaymentResponse response = PaymentResponse.builder()
                .status("ACTIVE")
                .gatewayNotice("Hệ thống chỉ thu phí kết nối. Số dư hiện tại: " + balance + "đ. Không quản lý học phí hoặc rút tiền cho gia sư.")
                .totalPendingFee(0.0)
                .totalPaidFee(balance.doubleValue())
                .invoices(Arrays.asList(
                        PaymentResponse.InvoiceItem.builder()
                                .id(101L)
                                .className("Số dư ví kết nối")
                                .period("Dùng để thanh toán phí kết nối sau khi gia sư chấp nhận lịch")
                                .amount(balance.doubleValue())
                                .status("BALANCE")
                                .dueDate("-")
                                .build(),
                        PaymentResponse.InvoiceItem.builder()
                                .id(100L)
                                .className("Phí kết nối mặc định")
                                .period("Thu một lần cho mỗi yêu cầu được gia sư chấp nhận")
                                .amount(50000.0)
                                .status("INFO")
                                .dueDate("-")
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
    @PostMapping("/sepay/register-booking")
    public ApiResponse<Map<String, Object>> registerPendingBooking(@RequestBody CreateClassRequest request) {
        if (request == null || request.getOrderCode() == null || request.getOrderCode().isBlank()) {
            return ApiResponse.error("Thiếu mã đơn hàng để lưu booking chờ thanh toán.");
        }
        String key = request.getOrderCode().toUpperCase().trim();
        pendingBookingRequests.put(key, request);
        Map<String, Object> data = new HashMap<>();
        data.put("orderCode", key);
        data.put("registered", true);
        return ApiResponse.success("Đã lưu booking chờ thanh toán thành công.", data);
    }

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

            CreateClassRequest bookingRequest = pendingBookingRequests.remove(key);
            if (bookingRequest != null) {
                try {
                    User bookingUser = null;
                    if (principal != null) {
                        String email = principal.getName();
                        bookingUser = userRepository.findByEmailIgnoreCase(email)
                                .or(() -> userRepository.findByEmail(email))
                                .orElse(null);
                    }
                    if (bookingUser == null && bookingRequest.getStudentId() != null) {
                        bookingUser = userRepository.findById(bookingRequest.getStudentId()).orElse(null);
                    }

                    if (bookingUser != null || bookingRequest.getStudentId() == null) {
                        ClassResponse created = tutoringClassService.createClass(bookingRequest, bookingUser);
                        data.put("bookingCreated", true);
                        data.put("classId", created != null ? created.getId() : null);
                    } else {
                        data.put("bookingCreated", false);
                        data.put("message", "Không thể tạo lớp học vì người dùng không tồn tại.");
                    }
                } catch (Exception ex) {
                    data.put("bookingCreated", false);
                    data.put("message", "Lỗi khi lưu lớp học sau thanh toán: " + ex.getMessage());
                }
            }

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

    @PostMapping("/deposit")
    public ApiResponse<Map<String, Object>> depositBalance(@RequestBody Map<String, Object> payload, Principal principal) {
        if (principal == null) {
            return ApiResponse.error("Vui lòng đăng nhập để nạp số dư kết nối.");
        }
        Number amountNumber = payload.get("amount") instanceof Number ? (Number) payload.get("amount") : null;
        if (amountNumber == null || amountNumber.doubleValue() <= 0) {
            return ApiResponse.error("Số tiền nạp không hợp lệ.");
        }
        User user = userRepository.findByEmailIgnoreCase(principal.getName())
                .or(() -> userRepository.findByEmail(principal.getName()))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng."));
        BigDecimal currentBalance = user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO;
        user.setBalance(currentBalance.add(BigDecimal.valueOf(amountNumber.doubleValue())));
        userRepository.save(user);

        Map<String, Object> data = new HashMap<>();
        data.put("balance", user.getBalance());
        return ApiResponse.success("Nạp số dư kết nối thành công.", data);
    }
}
