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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TutoringClassServiceImpl implements TutoringClassService {

    private final TutoringClassRepository tutoringClassRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;
    private final TutorRepository tutorRepository;
    private final StudentRepository studentRepository;
    private final ParentRepository parentRepository;
    private final LessonRepository lessonRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ClassResponse> getClassesForUser(User currentUser) {
        List<TutoringClass> classes;
        Role role = currentUser.getRole();

        if (role == Role.TUTOR) {
            classes = tutoringClassRepository.findByTutorIdOrderByCreatedAtDesc(currentUser.getId());
        } else if (role == Role.PARENT) {
            classes = tutoringClassRepository.findByParentIdOrderByCreatedAtDesc(currentUser.getId());
        } else if (role == Role.STUDENT) {
            classes = tutoringClassRepository.findByStudentIdOrderByCreatedAtDesc(currentUser.getId());
        } else {
            classes = tutoringClassRepository.findAll();
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
        if (currentUser.getRole() != Role.PARENT && currentUser.getRole() != Role.STUDENT) {
            throw new IllegalArgumentException("Chỉ phụ huynh hoặc học sinh được tạo yêu cầu kết nối gia sư.");
        }

        // 1. Resolve Subject (Môn học)
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
            for (Subject s : subjectRepository.findAll()) {
                if (request.getClassName().toLowerCase().contains(s.getName().toLowerCase())
                        || request.getClassName().toLowerCase().contains(s.getCode().toLowerCase())) {
                    subject = s;
                    break;
                }
            }
        }
        if (subject == null) {
            throw new IllegalArgumentException("Vui lòng chọn môn học hợp lệ cho yêu cầu kết nối.");
        }

        // 2. Resolve Tutor (Gia sư)
        if (request.getTutorId() == null) {
            throw new IllegalArgumentException("Vui lòng chọn gia sư cần kết nối.");
        }
        Tutor tutor = tutorRepository.findById(request.getTutorId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy gia sư với ID: " + request.getTutorId()));

        // 3. Resolve Student (Học sinh)
        Parent parent = null;
        Student student = null;
        if (currentUser != null && currentUser.getRole() == Role.STUDENT) {
            student = studentRepository.findById(currentUser.getId()).orElseGet(() -> {
                // Ensure record exists in students table for joined inheritance
                return studentRepository.save(Student.builder()
                        .id(currentUser.getId())
                        .email(currentUser.getEmail())
                        .fullName(currentUser.getFullName())
                        .password(currentUser.getPassword())
                        .phone(currentUser.getPhone())
                        .avatarUrl(currentUser.getAvatarUrl())
                        .role(Role.STUDENT)
                        .gradeLevel("Lớp 12")
                        .build());
            });
        } else if (currentUser != null && currentUser.getRole() == Role.PARENT) {
            parent = parentRepository.findById(currentUser.getId()).orElseGet(() -> {
                return parentRepository.save(Parent.builder()
                        .id(currentUser.getId())
                        .email(currentUser.getEmail())
                        .fullName(currentUser.getFullName())
                        .password(currentUser.getPassword())
                        .phone(currentUser.getPhone())
                        .avatarUrl(currentUser.getAvatarUrl())
                        .role(Role.PARENT)
                        .build());
            });

            if (request.getStudentId() != null) {
                final Long currentParentId = parent.getId();
                student = studentRepository.findById(request.getStudentId())
                        .filter(s -> s.getParent() != null && currentParentId.equals(s.getParent().getId()))
                        .orElse(null);
            }
            if (student == null) {
                String studentEmail = request.getStudentEmail();
                if (studentEmail == null || studentEmail.isBlank()) {
                    studentEmail = "student." + System.currentTimeMillis() + "@giasuhq.com";
                }
                student = studentRepository.save(Student.builder()
                        .email(studentEmail.trim().toLowerCase())
                        .fullName(request.getStudentName() != null && !request.getStudentName().isBlank() ? request.getStudentName() : currentUser.getFullName())
                        .password(currentUser.getPassword())
                        .phone(currentUser.getPhone())
                        .avatarUrl(currentUser.getAvatarUrl())
                        .role(Role.STUDENT)
                        .parent(parent)
                        .gradeLevel("Lớp 12")
                        .build());
            }
        } else if (request.getStudentId() != null) {
            student = studentRepository.findById(request.getStudentId()).orElse(null);
        }
        if (student == null) {
            throw new IllegalArgumentException("Không xác định được học sinh cho yêu cầu kết nối.");
        }

        // 4. Resolve Parent (Phụ huynh)
        if (parent == null && currentUser != null && currentUser.getRole() == Role.PARENT) {
            parent = parentRepository.findById(currentUser.getId()).orElseGet(() -> {
                return parentRepository.save(Parent.builder()
                        .id(currentUser.getId())
                        .email(currentUser.getEmail())
                        .fullName(currentUser.getFullName())
                        .password(currentUser.getPassword())
                        .phone(currentUser.getPhone())
                        .avatarUrl(currentUser.getAvatarUrl())
                        .role(Role.PARENT)
                        .build());
            });
        } else if (student.getParent() != null) {
            parent = student.getParent();
        }

        String scheduleDesc = request.getScheduleDescription();
        if (scheduleDesc == null || scheduleDesc.isBlank()) {
            if (request.getDate() != null && request.getTime() != null) {
                scheduleDesc = request.getDate() + " lúc " + request.getTime();
            } else {
                scheduleDesc = "Thứ 2 & Thứ 4 (18:00 - 20:00)";
            }
        }

        TutoringClass newClass = TutoringClass.builder()
                .className(request.getClassName())
                .subject(subject)
                .tutor(tutor)
                .student(student)
                .parent(parent)
                .scheduleDescription(scheduleDesc)
                .connectionFee(resolveConnectionFee(request))
                .status(ClassStatus.PENDING_TUTOR_APPROVAL)
                .build();

        TutoringClass savedClass = tutoringClassRepository.save(newClass);
        log.info("Class request created successfully with ID: {}", savedClass.getId());
        return mapToResponse(savedClass);
    }

    @Override
    @Transactional
    public ClassResponse acceptClass(Long id, User currentUser) {
        TutoringClass tutoringClass = getClassForTutorAction(id, currentUser);
        if (tutoringClass.getStatus() != ClassStatus.PENDING_TUTOR_APPROVAL) {
            throw new IllegalArgumentException("Chỉ yêu cầu đang chờ gia sư xác nhận mới được chấp nhận.");
        }
        tutoringClass.setStatus(ClassStatus.PENDING_PAYMENT);
        tutoringClass.setApprovedAt(LocalDateTime.now());
        return mapToResponse(tutoringClassRepository.save(tutoringClass));
    }

    @Override
    @Transactional
    public ClassResponse declineClass(Long id, User currentUser) {
        TutoringClass tutoringClass = getClassForTutorAction(id, currentUser);
        if (tutoringClass.getStatus() != ClassStatus.PENDING_TUTOR_APPROVAL) {
            throw new IllegalArgumentException("Chỉ yêu cầu đang chờ gia sư xác nhận mới được từ chối.");
        }
        tutoringClass.setStatus(ClassStatus.DECLINED);
        return mapToResponse(tutoringClassRepository.save(tutoringClass));
    }

    @Override
    @Transactional
    public ClassResponse payConnectionFee(Long id, User currentUser) {
        TutoringClass tutoringClass = tutoringClassRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học với ID: " + id));
        ensureOwner(tutoringClass, currentUser);
        if (tutoringClass.getStatus() != ClassStatus.PENDING_PAYMENT) {
            throw new IllegalArgumentException("Chỉ thanh toán được sau khi gia sư đã chấp nhận lịch học.");
        }
        BigDecimal fee = tutoringClass.getConnectionFee() != null ? tutoringClass.getConnectionFee() : BigDecimal.ZERO;
        BigDecimal balance = currentUser.getBalance() != null ? currentUser.getBalance() : BigDecimal.ZERO;
        if (balance.compareTo(fee) < 0) {
            throw new IllegalArgumentException("Số dư không đủ để thanh toán phí kết nối. Vui lòng nạp thêm.");
        }
        currentUser.setBalance(balance.subtract(fee));
        userRepository.save(currentUser);

        tutoringClass.setStatus(ClassStatus.ACTIVE);
        tutoringClass.setPaidAt(LocalDateTime.now());
        TutoringClass savedClass = tutoringClassRepository.save(tutoringClass);

        String datePart = null;
        String timePart = null;
        String scheduleDesc = savedClass.getScheduleDescription();
        if (scheduleDesc != null && scheduleDesc.contains(" lúc ")) {
            String[] parts = scheduleDesc.split(" lúc ");
            datePart = parts[0].trim();
            if (parts.length > 1) {
                timePart = parts[1].trim();
            }
        } else {
            datePart = scheduleDesc;
        }

        LocalDateTime startTime = parseStartTime(datePart, timePart);
        Lesson initialLesson = Lesson.builder()
                .tutoringClass(savedClass)
                .title("Buổi 1: " + savedClass.getClassName())
                .startTime(startTime)
                .endTime(startTime.plusHours(1))
                .status(LessonStatus.SCHEDULED)
                .build();
        lessonRepository.save(initialLesson);

        return mapToResponse(savedClass);
    }

    private BigDecimal resolveConnectionFee(CreateClassRequest request) {
        if (request.getAmount() != null && request.getAmount() > 0) {
            return BigDecimal.valueOf(request.getAmount());
        }
        return BigDecimal.valueOf(50000);
    }

    private TutoringClass getClassForTutorAction(Long id, User currentUser) {
        TutoringClass tutoringClass = tutoringClassRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học với ID: " + id));
        if (currentUser == null || currentUser.getRole() != Role.TUTOR
                || tutoringClass.getTutor() == null
                || !currentUser.getId().equals(tutoringClass.getTutor().getId())) {
            throw new IllegalArgumentException("Chỉ gia sư được gán với lớp này mới có quyền xử lý yêu cầu.");
        }
        return tutoringClass;
    }

    private void ensureCanView(TutoringClass tutoringClass, User currentUser) {
        if (currentUser == null) {
            throw new IllegalArgumentException("Chưa đăng nhập hoặc phiên làm việc đã hết hạn.");
        }
        if (currentUser.getRole() == Role.ADMIN) {
            return;
        }
        boolean canView = (tutoringClass.getTutor() != null && currentUser.getId().equals(tutoringClass.getTutor().getId()))
                || (tutoringClass.getStudent() != null && currentUser.getId().equals(tutoringClass.getStudent().getId()))
                || (tutoringClass.getParent() != null && currentUser.getId().equals(tutoringClass.getParent().getId()));
        if (!canView) {
            throw new IllegalArgumentException("Bạn không có quyền xem lớp học này.");
        }
    }

    private void ensureOwner(TutoringClass tutoringClass, User currentUser) {
        if (currentUser == null || (currentUser.getRole() != Role.PARENT && currentUser.getRole() != Role.STUDENT)) {
            throw new IllegalArgumentException("Chỉ phụ huynh hoặc học sinh tạo yêu cầu mới được thanh toán phí kết nối.");
        }
        boolean owner = (tutoringClass.getStudent() != null && currentUser.getId().equals(tutoringClass.getStudent().getId()))
                || (tutoringClass.getParent() != null && currentUser.getId().equals(tutoringClass.getParent().getId()));
        if (!owner) {
            throw new IllegalArgumentException("Bạn không có quyền thanh toán lớp học này.");
        }
    }

    private LocalDateTime parseStartTime(String dateStr, String timeStr) {
        try {
            LocalDate date;
            if (dateStr != null && !dateStr.isBlank()) {
                dateStr = dateStr.trim();
                if (dateStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
                    date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
                } else if (dateStr.contains("/")) {
                    String[] parts = dateStr.split("/");
                    if (parts.length == 3) {
                        date = LocalDate.of(Integer.parseInt(parts[2]), Integer.parseInt(parts[1]), Integer.parseInt(parts[0]));
                    } else {
                        date = LocalDate.now().plusDays(1);
                    }
                } else {
                    date = LocalDate.now().plusDays(1);
                }
            } else {
                date = LocalDate.now().plusDays(1);
            }

            LocalTime time = LocalTime.of(9, 0);
            if (timeStr != null && !timeStr.isBlank()) {
                String cleanTime = timeStr.trim().toLowerCase();
                if (cleanTime.contains("sa")) {
                    String t = cleanTime.replace("sa", "").trim();
                    String[] p = t.split(":");
                    int hour = Integer.parseInt(p[0].trim());
                    int min = p.length > 1 ? Integer.parseInt(p[1].trim()) : 0;
                    time = LocalTime.of(hour % 12, min);
                } else if (cleanTime.contains("ch")) {
                    String t = cleanTime.replace("ch", "").trim();
                    String[] p = t.split(":");
                    int hour = Integer.parseInt(p[0].trim());
                    int min = p.length > 1 ? Integer.parseInt(p[1].trim()) : 0;
                    time = LocalTime.of((hour % 12) + 12, min);
                } else if (cleanTime.matches("\\d{1,2}:\\d{2}")) {
                    String[] p = cleanTime.split(":");
                    time = LocalTime.of(Integer.parseInt(p[0]), Integer.parseInt(p[1]));
                }
            }

            return LocalDateTime.of(date, time);
        } catch (Exception e) {
            log.warn("Failed to parse start time from date: {}, time: {}, using fallback", dateStr, timeStr);
            return LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0);
        }
    }

    private ClassResponse mapToResponse(TutoringClass tc) {
        return ClassResponse.builder()
                .id(tc.getId())
                .className(tc.getClassName())
                .tutorId(tc.getTutor() != null ? tc.getTutor().getId() : null)
                .tutorName(tc.getTutor() != null ? tc.getTutor().getFullName() : "Gia sư")
                .studentId(tc.getStudent() != null ? tc.getStudent().getId() : null)
                .studentName(tc.getStudent() != null ? tc.getStudent().getFullName() : "Học sinh")
                .parentId(tc.getParent() != null ? tc.getParent().getId() : null)
                .parentName(tc.getParent() != null ? tc.getParent().getFullName() : "Chưa gắn Phụ huynh")
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
