package com.giasuhq.service.impl;

import com.giasuhq.dto.request.CreateClassRequest;
import com.giasuhq.dto.response.ClassResponse;
import com.giasuhq.entity.*;
import com.giasuhq.exception.ResourceNotFoundException;
import com.giasuhq.repository.*;
import com.giasuhq.service.TutoringClassService;
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

    @Override
    @Transactional(readOnly = true)
    public List<ClassResponse> getClassesForUser(User currentUser) {
        List<TutoringClass> classes;
        Role role = currentUser.getRole();

        if (role == Role.ADMIN) {
            classes = tutoringClassRepository.findAllByOrderByCreatedAtDesc();
        } else if (role == Role.TUTOR) {
            classes = tutoringClassRepository.findByTutorIdOrderByCreatedAtDesc(currentUser.getId());
        } else {
            classes = tutoringClassRepository.findByParentIdOrderByCreatedAtDesc(currentUser.getId());
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
            throw new IllegalArgumentException("Vui lòng đăng nhập để tạo yêu cầu kết nối gia sư.");
        }
        if (currentUser.getRole() != Role.PARENT && currentUser.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("Chỉ phụ huynh hoặc quản trị viên được tạo yêu cầu kết nối gia sư.");
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
            throw new IllegalArgumentException("Vui lòng chọn môn học hợp lệ cho yêu cầu kết nối.");
        }
        log.info("Resolved subject: id={}, name={}", subject.getId(), subject.getName());

        // 2. Resolve Tutor
        if (request.getTutorId() == null) {
            throw new IllegalArgumentException("Vui lòng chọn gia sư cần kết nối.");
        }
        Tutor tutor = tutorRepository.findById(request.getTutorId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy gia sư với ID: " + request.getTutorId()));

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

        if (parent == null && currentUser.getRole() == Role.ADMIN) {
            parent = parentRepository.findAll().stream().findFirst().orElse(null);
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
                : new BigDecimal("50000");

        TutoringClass tutoringClass = TutoringClass.builder()
                .className(request.getClassName())
                .tutor(tutor)
                .studentName(studentName)
                .studentGradeLevel(studentGradeLevel)
                .studentSchoolName(studentSchoolName)
                .parent(parent)
                .subject(subject)
                .scheduleDescription(scheduleDesc)
                .connectionFee(connectionFee)
                .status(ClassStatus.PENDING_TUTOR_APPROVAL)
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

        tutoringClass.setStatus(ClassStatus.PENDING_PAYMENT);
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
    public ClassResponse payConnectionFee(Long id, User currentUser) {
        TutoringClass tutoringClass = tutoringClassRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học với ID: " + id));

        ensureOwner(tutoringClass, currentUser);

        if (tutoringClass.getStatus() != ClassStatus.PENDING_PAYMENT) {
            throw new IllegalArgumentException("Lớp học không ở trạng thái chờ thanh toán phí kết nối (Trạng thái hiện tại: " + tutoringClass.getStatus() + ").");
        }

        BigDecimal fee = tutoringClass.getConnectionFee() != null && tutoringClass.getConnectionFee().compareTo(BigDecimal.ZERO) > 0
                ? tutoringClass.getConnectionFee()
                : new BigDecimal("50000");

        User payer = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin người dùng thanh toán."));

        BigDecimal currentBalance = payer.getBalance() != null ? payer.getBalance() : BigDecimal.ZERO;
        if (currentBalance.compareTo(fee) < 0) {
            throw new IllegalArgumentException("Số dư ví kết nối không đủ để thanh toán ("
                    + String.format("%,.0fđ", currentBalance) + " < " + String.format("%,.0fđ", fee)
                    + "). Vui lòng nạp thêm tiền vào ví tại mục Ví kết nối.");
        }

        payer.setBalance(currentBalance.subtract(fee));
        userRepository.save(payer);

        tutoringClass.setStatus(ClassStatus.ACTIVE);
        tutoringClass.setPaidAt(LocalDateTime.now());
        TutoringClass saved = tutoringClassRepository.save(tutoringClass);
        return mapToResponse(saved);
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
 
    try {
        // ----- Ngày -----
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
            } else if (vn.find()) { // find() nên tự bỏ qua tiền tố "T2 ", "CN "...
                date = LocalDate.of(Integer.parseInt(vn.group(3)),
                        Integer.parseInt(vn.group(2)),
                        Integer.parseInt(vn.group(1)));
            } else {
                throw new IllegalArgumentException("Ngày không hợp lệ: " + dateStr);
            }
        }
 
        // ----- Giờ -----
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
                if (hour < 12) hour += 12;        // 2 CH -> 14, 12 CH -> 12
            } else if ("sa".equals(period) || "am".equals(period)) {
                if (hour == 12) hour = 0;         // 12 SA -> 0
            }
            time = LocalTime.of(hour, minute);
        }
 
        return LocalDateTime.of(date, time);
 
    } catch (DateTimeException | NumberFormatException e) {
        throw new IllegalArgumentException(
                "Không đọc được ngày giờ: date=" + dateStr + ", time=" + timeStr, e);
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
                .approvedAt(tc.getApprovedAt())
                .paidAt(tc.getPaidAt())
                .status(tc.getStatus())
                .createdAt(tc.getCreatedAt())
                .build();
    }
}
