package com.giasuhq.service;

import com.giasuhq.dto.request.CreateClassRequest;
import com.giasuhq.dto.response.ClassResponse;
import com.giasuhq.dto.response.ConnectionPaymentResponse;
import com.giasuhq.entity.User;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface TutoringClassService {
    List<ClassResponse> getClassesForUser(User currentUser);
    ClassResponse getClassById(Long id, User currentUser);
    ClassResponse createClass(CreateClassRequest request, User currentUser);
    ClassResponse acceptClass(Long id, User currentUser);
    ClassResponse declineClass(Long id, User currentUser);
    ConnectionPaymentResponse payConnectionFee(Long id, User currentUser, String voucherCode);
    ConnectionPaymentResponse recordExternalConnectionPayment(Long id, User currentUser, BigDecimal transferredAmount, String voucherCode);
    ConnectionPaymentResponse createAndRecordExternalConnectionPayment(
            CreateClassRequest request,
            User currentUser,
            BigDecimal transferredAmount
    );
    Optional<ClassResponse> findClassByPaymentOrderCode(String paymentOrderCode, User currentUser);
}
