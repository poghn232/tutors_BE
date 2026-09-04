package com.giasuhq.controller;

import com.giasuhq.dto.response.ApiResponse;
import com.giasuhq.dto.response.PaymentResponse;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @GetMapping
    public ApiResponse<PaymentResponse> getPaymentOverview() {
        PaymentResponse response = PaymentResponse.builder()
                .status("API_GATEWAY_PENDING")
                .gatewayNotice("⚡ Chức năng Thanh toán trực tuyến qua cổng API (VNPay / ZaloPay / MoMo) đang chuẩn bị kết nối. Hiện tại hệ thống tự động theo dõi danh sách học phí.")
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

    @PostMapping("/checkout")
    public ApiResponse<String> processCheckout(@RequestParam Long invoiceId) {
        return ApiResponse.success("Yêu cầu thanh toán cho hóa đơn #" + invoiceId + " đã được ghi nhận. Cổng API VNPay sẽ xử lý khi khởi chạy chính thức.", null);
    }
}
