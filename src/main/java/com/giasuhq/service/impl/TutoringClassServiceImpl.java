package com.giasuhq.service.impl;

import com.giasuhq.dto.request.CreateClassRequest;
import com.giasuhq.dto.response.ClassResponse;
import com.giasuhq.dto.response.ConnectionPaymentResponse;
import com.giasuhq.dto.response.VoucherQuoteResponse;
import com.giasuhq.dto.response.VoucherRedemptionResult;
import com.giasuhq.dto.response.VoucherResponse;
import com.giasuhq.entity.*;
import com.giasuhq.exception.ResourceNotFoundException;
import com.giasuhq.repository.*;
import com.giasuhq.service.TutoringClassService;
import com.giasuhq.service.VoucherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TutoringClassServiceImpl implements TutoringClassService {

    private final TutoringClassRepository tutoringClassRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;
    private final TutorRepository tutorRepository;
    private final ParentRepository parentRepository;
    private final LessonRepository lessonRepository;
    private final VoucherService voucherService;

    @Override
    @Transactional
    public List<ClassResponse> getClassesForUser(User currentUser) {
        List<TutoringClass> classes;
        Role role = currentUser.getRole();

        if (role == Role.ADMIN) {
            classes = tutoringClassRepository.findAllByOrderByCreatedAtDesc();
        } else if (role == Role.TUTOR) {
            classes = new ArrayList<>(tutoringClassRepository.findByTutorIdOrderByCreatedAtDesc(currentUser.getId()));
            // Checkout bookings are stored before SePay confirms the transfer
            // so a browser refresh or deploy can recover them. They must not
            // appear in the tutor queue until the upfront fee is recorded.
            classes.removeIf(this::isUnpaidCheckoutBooking);
            boolean repairedLegacyPaidRequests = false;
            for (TutoringClass tutoringClass : classes) {
                if (hasRecordedConnectionPayment(tutoringClass)
                        && tutoringClass.getApprovedAt() == null
                        && tutoringClass.getStatus() != ClassStatus.DECLINED
                        && tutoringClass.getStatus() != ClassStatus.CANCELLED
                        && tutoringClass.getStatus() != ClassStatus.COMPLETED) {
                    tutoringClass.setStatus(ClassStatus.PENDING_TUTOR_APPROVAL);
                    repairedLegacyPaidRequests = true;
                }
            }
            if (repairedLegacyPaidRequests) {
                classes = tutoringClassRepository.saveAll(classes);
            }
        } else {
            classes = tutoringClassRepository.findByParentIdOrderByCreatedAtDesc(currentUser.getId());
        }

        boolean repairedPaymentStates = false;
        for (TutoringClass tutoringClass : classes) {
            if (!hasRecordedConnectionPayment(tutoringClass)
                    || tutoringClass.getStatus() == ClassStatus.DECLINED
                    || tutoringClass.getStatus() == ClassStatus.CANCELLED
                    || tutoringClass.getStatus() == ClassStatus.COMPLETED) {
                continue;
            }

            if (tutoringClass.getApprovedAt() == null
                    && tutoringClass.getStatus() != ClassStatus.PENDING_TUTOR_APPROVAL) {
                tutoringClass.setStatus(ClassStatus.PENDING_TUTOR_APPROVAL);
                repairedPaymentStates = true;
            } else if (tutoringClass.getApprovedAt() != null
                    && tutoringClass.getStatus() == ClassStatus.PENDING_PAYMENT) {
                tutoringClass.setStatus(ClassStatus.ACTIVE);
                repairedPaymentStates = true;
            }
        }
        if (repairedPaymentStates) {
            classes = tutoringClassRepository.saveAll(classes);
        }

        return classes.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ClassResponse getClassById(Long id, User currentUser) {
        TutoringClass tutoringClass = tutoringClassRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học với ID: " + id));
        ensureCanView(tutoringClass, currentUser);
        return mapToResponse(tutoringClass);
    }

    @Override
    @Transactional
    public ClassResponse createClass(CreateClassRequest request, User currentUser) {
        log.info("Creating class request: {}, currentUser: {}", request, currentUser != null ? currentUser.getEmail() : "anonymous");
        if (currentUser == null) {
            throw new IllegalArgumentException("Vui lòng đăng nhập để tạo lớp học hoặc thiết lập lịch rảnh.");
        }

        // 1. Resolve Subject
        Subject subject = null;
        if (request.getSubjectId() != null) {
            subject = subjectRepository.findById(request.getSubjectId()).orElse(null);
        }
        if (subject == null && request.getSubjectName() != null && !request.getSubjectName().isBlank()) {
            String name = request.getSubjectName().trim();
            subject = subjectRepository.findByNameContainingIgnoreCase(name)
                    .or(() -> subjectRepository.findByCode(name.toUpperCase()))
                    .orElse(null);
        }
        if (subject == null && request.getClassName() != null) {
            String className = request.getClassName().trim();
            for (Subject s : subjectRepository.findAll()) {
                if (className.toLowerCase().contains(s.getName().toLowerCase())
                        || className.toLowerCase().contains(s.getCode().toLowerCase())) {
                    subject = s;
                    break;
                }
            }
        }
        if (subject == null) {
            // Fallback to first available subject (MATH) if not specified
            subject = subjectRepository.findAll().stream().findFirst().orElse(null);
        }
        if (subject == null) {
            throw new IllegalArgumentException("Vui lòng chọn môn học hợp lệ cho lớp học.");
        }
        log.info("Resolved subject: id={}, name={}", subject.getId(), subject.getName());

        // 2. Resolve Tutor
        Tutor tutor = null;
        if (currentUser.getRole() == Role.TUTOR) {
            tutor = tutorRepository.findById(currentUser.getId()).orElseGet(() ->
                    tutorRepository.save(Tutor.builder()
                            .id(currentUser.getId())
                            .email(currentUser.getEmail())
                            .fullName(currentUser.getFullName())
                            .phone(currentUser.getPhone())
                            .role(Role.TUTOR)
                            .build())
            );
        } else {
            Long tutorId = request.getTutorId();
            if (tutorId == null) {
                throw new IllegalArgumentException("Vui lòng chọn đúng gia sư cho yêu cầu kết nối.");
            } else {
                tutor = tutorRepository.findById(tutorId).orElse(null);
            }
            if (tutor == null) {
                throw new IllegalArgumentException("Không tìm thấy gia sư đã chọn trong hệ thống.");
            }
        }
        if (tutor == null) {
            throw new IllegalArgumentException("Không tìm thấy gia sư hợp lệ trong hệ thống.");
        }

        // 3. Resolve Parent
        Parent parent = null;
        if (currentUser.getRole() == Role.PARENT) {
            parent = parentRepository.findById(currentUser.getId()).orElseGet(() ->
                    parentRepository.save(Parent.builder()
                            .id(currentUser.getId())
                            .email(currentUser.getEmail())
                            .fullName(currentUser.getFullName())
                            .phone(currentUser.getPhone())
                            .role(Role.PARENT)
                            .balance(currentUser.getBalance() != null ? currentUser.getBalance() : BigDecimal.ZERO)
                            .build())
            );
        } else if (request.getParentId() != null) {
            parent = parentRepository.findById(request.getParentId()).orElse(null);
        }

        // 4. Resolve Student information
        String studentName = request.getStudentName() != null && !request.getStudentName().isBlank()
                ? request.getStudentName().trim()
                : (parent != null && parent.getStudentName() != null && !parent.getStudentName().isBlank()
                    ? parent.getStudentName().trim()
                    : currentUser.getFullName());

        String studentGradeLevel = parent != null ? parent.getStudentGradeLevel() : null;
        String studentSchoolName = parent != null ? parent.getStudentSchoolName() : null;

        // 5. Schedule & Connection Fee
        String scheduleDesc = request.getScheduleDescription();
        if (scheduleDesc == null || scheduleDesc.isBlank()) {
            if (request.getDate() != null || request.getTime() != null) {
                scheduleDesc = (request.getDate() != null ? request.getDate() : "") +
                        (request.getTime() != null ? " (" + request.getTime() + ")" : "");
            } else {
                scheduleDesc = "Lịch học linh hoạt theo thỏa thuận";
            }
        }

        BigDecimal connectionFee = request.getAmount() != null && request.getAmount() > 0
                ? BigDecimal.valueOf(request.getAmount())
                : new BigDecimal("5000");

        String paymentOrderCode = normalizePaymentOrderCode(request.getOrderCode());

        ClassStatus initialStatus = ClassStatus.ACTIVE;
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            try {
                initialStatus = ClassStatus.valueOf(request.getStatus().trim().toUpperCase());
            } catch (Exception e) {
                initialStatus = ClassStatus.ACTIVE;
            }
        } else if (currentUser.getRole() == Role.PARENT && paymentOrderCode != null) {
            // The connection fee is paid before the tutor sees or accepts the
            // request. The payment callback changes this to
            // PENDING_TUTOR_APPROVAL after recording the transfer.
            initialStatus = ClassStatus.PENDING_PAYMENT;
        }

        TutoringClass tutoringClass = TutoringClass.builder()
                .className(request.getClassName() != null && !request.getClassName().isBlank() ? request.getClassName() : ("Lớp " + subject.getName() + " cùng " + tutor.getFullName()))
                .tutor(tutor)
                .studentName(studentName)
                .studentGradeLevel(studentGradeLevel)
                .studentSchoolName(studentSchoolName)
                .parent(parent)
                .subject(subject)
                .scheduleDescription(scheduleDesc)
                .connectionFee(connectionFee)
                .paymentOrderCode(paymentOrderCode)
                .status(initialStatus)
                .build();

        TutoringClass savedClass = tutoringClassRepository.save(tutoringClass);

        // 6. Automatically generate initial pending lesson
        try {
            LocalDateTime startDateTime = parseStartTime(request.getDate(), request.getTime());
            LocalDateTime endDateTime = startDateTime.plusHours(2);

            Lesson lesson = Lesson.builder()
                    .tutoringClass(savedClass)
                    .title("Buổi 1: Làm quen và kiểm tra trình độ (" + subject.getName() + ")")
                    .startTime(startDateTime)
                    .endTime(endDateTime)
                    .status(LessonStatus.SCHEDULED)
                    .build();
            lessonRepository.save(lesson);
        } catch (Exception e) {
            log.warn("Failed to create initial lesson for class {}: {}", savedClass.getId(), e.getMessage());
        }

        return mapToResponse(savedClass);
    }

    @Override
    @Transactional
    public ClassResponse acceptClass(Long id, User currentUser) {
        TutoringClass tutoringClass = tutoringClassRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học với ID: " + id));

        if (currentUser.getRole() != Role.ADMIN) {
            if (tutoringClass.getTutor() == null || !currentUser.getId().equals(tutoringClass.getTutor().getId())) {
                throw new IllegalArgumentException("Chỉ gia sư của lớp học mới có quyền chấp nhận yêu cầu này.");
            }
        }

        if (tutoringClass.getStatus() != ClassStatus.PENDING_TUTOR_APPROVAL) {
            throw new IllegalArgumentException("Lớp học không ở trạng thái chờ duyệt (Trạng thái hiện tại: " + tutoringClass.getStatus() + ").");
        }

        if (!hasRecordedConnectionPayment(tutoringClass)) {
            throw new IllegalArgumentException(
                    "Chưa ghi nhận phí kết nối ban đầu. Phụ huynh cần hoàn tất thanh toán trước khi gia sư xác nhận lịch học."
            );
        }

        // The connection fee has already been paid during booking. Tutor
        // acceptance only activates the class; it never starts another charge.
        tutoringClass.setStatus(ClassStatus.ACTIVE);
        tutoringClass.setApprovedAt(LocalDateTime.now());
        TutoringClass saved = tutoringClassRepository.save(tutoringClass);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public ClassResponse declineClass(Long id, User currentUser) {
        TutoringClass tutoringClass = tutoringClassRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học với ID: " + id));

        if (currentUser.getRole() != Role.ADMIN) {
            if (tutoringClass.getTutor() == null || !currentUser.getId().equals(tutoringClass.getTutor().getId())) {
                throw new IllegalArgumentException("Chỉ gia sư của lớp học mới có quyền từ chối yêu cầu này.");
            }
        }

        if (tutoringClass.getStatus() != ClassStatus.PENDING_TUTOR_APPROVAL) {
            throw new IllegalArgumentException("Không thể từ chối lớp học ở trạng thái: " + tutoringClass.getStatus());
        }

        tutoringClass.setStatus(ClassStatus.DECLINED);
        TutoringClass saved = tutoringClassRepository.save(tutoringClass);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public ConnectionPaymentResponse payConnectionFee(Long id, User currentUser, String voucherCode) {
        TutoringClass tutoringClass = tutoringClassRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học với ID: " + id));

        ensureOwner(tutoringClass, currentUser);

        if (hasRecordedConnectionPayment(tutoringClass)) {
            BigDecimal fee = effectiveConnectionFee(tutoringClass);
            return buildPaymentResponse(tutoringClass, fee, null,
                    tutoringClass.getPaidAmount() != null ? tutoringClass.getPaidAmount() : fee, null);
        }

        // Kept for old clients so a stale bundle cannot charge the parent a
        // second time. New bookings are always paid in CheckoutFlow/SePay.
        throw new IllegalArgumentException(
                "Phí kết nối được thanh toán một lần ở bước đăng ký. Không cần thanh toán lại sau khi gia sư xác nhận."
        );
    }

    @Override
    @Transactional
    public ConnectionPaymentResponse recordExternalConnectionPayment(
            Long id,
            User currentUser,
            BigDecimal transferredAmount,
            String voucherCode
    ) {
        TutoringClass tutoringClass = tutoringClassRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học với ID: " + id));
        ensureOwner(tutoringClass, currentUser);

        if (hasRecordedConnectionPayment(tutoringClass)) {
            BigDecimal fee = effectiveConnectionFee(tutoringClass);
            return buildPaymentResponse(tutoringClass, fee, null,
                    tutoringClass.getPaidAmount() != null ? tutoringClass.getPaidAmount() : fee, null);
        }
        if (tutoringClass.getStatus() != ClassStatus.PENDING_TUTOR_APPROVAL
                && tutoringClass.getStatus() != ClassStatus.PENDING_PAYMENT
                && tutoringClass.getStatus() != ClassStatus.ACTIVE) {
            throw new IllegalArgumentException("Lớp học không ở trạng thái có thể ghi nhận thanh toán phí kết nối.");
        }

        BigDecimal fee = effectiveConnectionFee(tutoringClass);
        VoucherQuoteResponse quote = quoteVoucher(currentUser, voucherCode, fee);
        BigDecimal finalAmount = quote.getFinalAmount();
        BigDecimal actualAmount = transferredAmount != null ? transferredAmount : BigDecimal.ZERO;
        if (actualAmount.compareTo(finalAmount) < 0) {
            throw new IllegalArgumentException("Số tiền chuyển khoản chưa đủ sau khi áp dụng voucher (cần "
                    + String.format("%,.0fđ", finalAmount) + ").");
        }

        VoucherRedemptionResult redemption = redeemVoucher(currentUser, voucherCode, fee, tutoringClass.getId());
        tutoringClass.setDiscountAmount(redemption != null ? redemption.getDiscountAmount() : BigDecimal.ZERO);
        tutoringClass.setPaidAmount(finalAmount);
        tutoringClass.setVoucherCode(redemption != null ? redemption.getCode() : null);
        tutoringClass.setPaidAt(LocalDateTime.now());
        // Payment and tutor acceptance are separate business decisions.
        // approvedAt is the durable source of truth for tutor acceptance:
        // without it, a paid request must remain visible in the tutor queue.
        if (tutoringClass.getApprovedAt() == null) {
            tutoringClass.setStatus(ClassStatus.PENDING_TUTOR_APPROVAL);
        } else {
            tutoringClass.setStatus(ClassStatus.ACTIVE);
        }
        TutoringClass saved = tutoringClassRepository.save(tutoringClass);
        // Voucher issuance is optional and runs in its own transaction so a
        // promotion-table problem cannot roll back the already-recorded fee.
        VoucherResponse issuedVoucher = null;
        try {
            issuedVoucher = voucherService.issuePostPaymentVoucher(currentUser);
        } catch (RuntimeException ex) {
            log.warn("Could not issue post-payment voucher for user {}: {}", currentUser.getId(), ex.getMessage());
        }
        return buildPaymentResponse(saved, fee, redemption, finalAmount, issuedVoucher);
    }

    @Override
    @Transactional
    public synchronized ConnectionPaymentResponse createAndRecordExternalConnectionPayment(
            CreateClassRequest request,
            User currentUser,
            BigDecimal transferredAmount
    ) {
        if (request == null) {
            throw new IllegalArgumentException("Thiếu thông tin booking cần ghi nhận thanh toán.");
        }
        String paymentOrderCode = normalizePaymentOrderCode(request.getOrderCode());
        if (paymentOrderCode == null) {
            throw new IllegalArgumentException("Thiếu mã đơn hàng để ghi nhận thanh toán.");
        }

        // The payment order is the idempotency boundary. A later poll returns
        // the already-paid class instead of creating a new class or voucher.
        TutoringClass existingClass = tutoringClassRepository
                .findByPaymentOrderCodeIgnoreCase(paymentOrderCode)
                .orElse(null);
        if (existingClass != null) {
            ensureOwner(existingClass, currentUser);
            if (hasRecordedConnectionPayment(existingClass)) {
                BigDecimal fee = effectiveConnectionFee(existingClass);
                return buildPaymentResponse(
                        existingClass,
                        fee,
                        null,
                        existingClass.getPaidAmount() != null ? existingClass.getPaidAmount() : fee,
                        null
                );
            }
            return recordExternalConnectionPayment(
                    existingClass.getId(),
                    currentUser,
                    transferredAmount,
                    request.getVoucherCode()
            );
        }

        // The platform fee is server-controlled; client-provided amount is
        // ignored before creating the pending class.
        request.setAmount(5000.0);
        request.setPaymentAmount(transferredAmount != null ? transferredAmount.doubleValue() : null);
        request.setOrderCode(paymentOrderCode);

        ClassResponse created = createClass(request, currentUser);
        return recordExternalConnectionPayment(
                created.getId(),
                currentUser,
                transferredAmount,
                request.getVoucherCode()
        );
    }

    private boolean hasRecordedConnectionPayment(TutoringClass tutoringClass) {
        return tutoringClass.getPaidAt() != null
                || (tutoringClass.getPaidAmount() != null
                && tutoringClass.getPaidAmount().compareTo(BigDecimal.ZERO) > 0);
    }

    private boolean isUnpaidCheckoutBooking(TutoringClass tutoringClass) {
        boolean hasCheckoutOrder = tutoringClass.getPaymentOrderCode() != null
                && !tutoringClass.getPaymentOrderCode().isBlank();
        return !hasRecordedConnectionPayment(tutoringClass)
                && (hasCheckoutOrder || tutoringClass.getStatus() == ClassStatus.PENDING_TUTOR_APPROVAL);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ClassResponse> findClassByPaymentOrderCode(String paymentOrderCode, User currentUser) {
        String normalizedCode = normalizePaymentOrderCode(paymentOrderCode);
        if (normalizedCode == null) {
            return Optional.empty();
        }
        return tutoringClassRepository.findByPaymentOrderCodeIgnoreCase(normalizedCode)
                .map(tutoringClass -> {
                    ensureOwner(tutoringClass, currentUser);
                    return mapToResponse(tutoringClass);
                });
    }

    private VoucherQuoteResponse quoteVoucher(User user, String voucherCode, BigDecimal fee) {
        if (voucherCode == null || voucherCode.isBlank()) {
            return VoucherQuoteResponse.builder()
                    .valid(true)
                    .originalAmount(fee)
                    .discountAmount(BigDecimal.ZERO)
                    .finalAmount(fee)
                    .build();
        }
        return voucherService.quote(user, voucherCode, fee);
    }

    private VoucherRedemptionResult redeemVoucher(User user, String voucherCode, BigDecimal fee, Long classId) {
        if (voucherCode == null || voucherCode.isBlank()) {
            return null;
        }
        return voucherService.redeem(user, voucherCode, fee, classId);
    }

    private BigDecimal effectiveConnectionFee(TutoringClass tutoringClass) {
        return tutoringClass.getConnectionFee() != null
                && tutoringClass.getConnectionFee().compareTo(BigDecimal.ZERO) > 0
                ? tutoringClass.getConnectionFee()
                : new BigDecimal("5000");
    }

    private String normalizePaymentOrderCode(String orderCode) {
        if (orderCode == null || orderCode.isBlank()) {
            return null;
        }
        return orderCode.trim().toUpperCase(Locale.ROOT);
    }

    private ConnectionPaymentResponse buildPaymentResponse(
            TutoringClass tutoringClass,
            BigDecimal fee,
            VoucherRedemptionResult redemption,
            BigDecimal finalAmount,
            VoucherResponse issuedVoucher
    ) {
        return ConnectionPaymentResponse.builder()
                .classInfo(mapToResponse(tutoringClass))
                .originalAmount(fee)
                .discountAmount(redemption != null ? redemption.getDiscountAmount() : BigDecimal.ZERO)
                .finalAmount(finalAmount)
                .voucherCode(redemption != null ? redemption.getCode() : tutoringClass.getVoucherCode())
                .issuedVoucher(issuedVoucher)
                .build();
    }

    private void ensureCanView(TutoringClass tutoringClass, User currentUser) {
        if (currentUser == null) {
            throw new IllegalArgumentException("Chưa đăng nhập hoặc phiên làm việc đã hết hạn.");
        }
        if (currentUser.getRole() == Role.ADMIN) {
            return;
        }
        boolean canView = (tutoringClass.getTutor() != null && currentUser.getId().equals(tutoringClass.getTutor().getId()))
                || (tutoringClass.getParent() != null && currentUser.getId().equals(tutoringClass.getParent().getId()));
        if (!canView) {
            throw new IllegalArgumentException("Bạn không có quyền xem lớp học này.");
        }
    }

    private void ensureOwner(TutoringClass tutoringClass, User currentUser) {
        if (currentUser == null) {
            throw new IllegalArgumentException("Chưa đăng nhập hoặc phiên làm việc đã hết hạn.");
        }
        if (currentUser.getRole() == Role.ADMIN) {
            return;
        }
        if (currentUser.getRole() != Role.PARENT) {
            throw new IllegalArgumentException("Chỉ phụ huynh tạo yêu cầu mới được thanh toán phí kết nối.");
        }
        boolean owner = (tutoringClass.getParent() != null && currentUser.getId().equals(tutoringClass.getParent().getId()));
        if (!owner) {
            throw new IllegalArgumentException("Bạn không có quyền thanh toán lớp học này.");
        }
    }

    private static final Pattern ISO_DATE = Pattern.compile("(\\d{4})-(\\d{1,2})-(\\d{1,2})");
    private static final Pattern VN_DATE  = Pattern.compile("(\\d{1,2})/(\\d{1,2})/(\\d{4})");
    private static final Pattern TIME     = Pattern.compile("(\\d{1,2})(?::(\\d{2}))?\\s*(sa|ch|am|pm)?", Pattern.CASE_INSENSITIVE);

    public LocalDateTime parseStartTime(String dateStr, String timeStr) {
        boolean noDate = dateStr == null || dateStr.isBlank();
        boolean noTime = timeStr == null || timeStr.isBlank();

        if (noDate && noTime) {
            return LocalDate.now().plusDays(1).atTime(9, 0);
        }

        try {
            LocalDate date;
            if (noDate) {
                date = LocalDate.now().plusDays(1);
            } else {
                String d = dateStr.trim();
                Matcher iso = ISO_DATE.matcher(d);
                Matcher vn = VN_DATE.matcher(d);
                if (iso.find()) {
                    date = LocalDate.of(Integer.parseInt(iso.group(1)),
                            Integer.parseInt(iso.group(2)),
                            Integer.parseInt(iso.group(3)));
                } else if (vn.find()) {
                    date = LocalDate.of(Integer.parseInt(vn.group(3)),
                            Integer.parseInt(vn.group(2)),
                            Integer.parseInt(vn.group(1)));
                } else {
                    throw new IllegalArgumentException("Ngày không hợp lệ: " + dateStr);
                }
            }

            LocalTime time;
            if (noTime) {
                time = LocalTime.of(9, 0);
            } else {
                Matcher m = TIME.matcher(timeStr.trim());
                if (!m.matches()) {
                    throw new IllegalArgumentException("Giờ không hợp lệ: " + timeStr);
                }
                int hour = Integer.parseInt(m.group(1));
                int minute = m.group(2) != null ? Integer.parseInt(m.group(2)) : 0;
                String period = m.group(3) != null ? m.group(3).toLowerCase() : null;

                if ("ch".equals(period) || "pm".equals(period)) {
                    if (hour < 12) hour += 12;
                } else if ("sa".equals(period) || "am".equals(period)) {
                    if (hour == 12) hour = 0;
                }
                time = LocalTime.of(hour, minute);
            }

            return LocalDateTime.of(date, time);
        } catch (DateTimeException | NumberFormatException e) {
            throw new IllegalArgumentException("Không đọc được ngày giờ: date=" + dateStr + ", time=" + timeStr, e);
        }
    }



    private ClassResponse mapToResponse(TutoringClass tc) {
        return ClassResponse.builder()
                .id(tc.getId())
                .className(tc.getClassName())
                .tutorId(tc.getTutor() != null ? tc.getTutor().getId() : null)
                .tutorName(tc.getTutor() != null ? tc.getTutor().getFullName() : "Gia sư")
                .studentId(null)
                .studentName(tc.getStudentName() != null ? tc.getStudentName() : (tc.getParent() != null ? tc.getParent().getStudentName() : "Học sinh"))
                .parentId(tc.getParent() != null ? tc.getParent().getId() : null)
                .parentName(tc.getParent() != null ? tc.getParent().getFullName() : "Phụ huynh")
                .subjectId(tc.getSubject() != null ? tc.getSubject().getId() : null)
                .subjectName(tc.getSubject() != null ? tc.getSubject().getName() : "Môn học")
                .scheduleDescription(tc.getScheduleDescription())
                .connectionFee(tc.getConnectionFee())
                .discountAmount(tc.getDiscountAmount() != null ? tc.getDiscountAmount() : BigDecimal.ZERO)
                .paidAmount(tc.getPaidAmount())
                .voucherCode(tc.getVoucherCode())
                .paymentOrderCode(tc.getPaymentOrderCode())
                .approvedAt(tc.getApprovedAt())
                .paidAt(tc.getPaidAt())
                .status(tc.getStatus())
                .createdAt(tc.getCreatedAt())
                .build();
    }
}
