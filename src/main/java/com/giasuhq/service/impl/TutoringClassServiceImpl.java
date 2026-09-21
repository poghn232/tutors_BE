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
            classes = tutoringClassRepository.findByTutorId(currentUser.getId());
        } else if (role == Role.PARENT) {
            classes = tutoringClassRepository.findByParentId(currentUser.getId());
        } else if (role == Role.STUDENT) {
            classes = tutoringClassRepository.findByStudentId(currentUser.getId());
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
        return mapToResponse(tutoringClass);
    }

    @Override
    @Transactional
    public ClassResponse createClass(CreateClassRequest request, User currentUser) {
        log.info("Creating class request: {}, currentUser: {}", request, currentUser != null ? currentUser.getEmail() : "anonymous");

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
            throw new IllegalArgumentException("Thiếu thông tin môn học. Vui lòng chọn môn học trước khi đặt lịch.");
        }

        // 2. Resolve Tutor (Gia sư)
        Tutor tutor = null;
        if (currentUser != null && currentUser.getRole() == Role.TUTOR) {
            tutor = tutorRepository.findById(currentUser.getId()).orElse(null);
        }
        if (tutor == null && request.getTutorId() != null) {
            tutor = tutorRepository.findById(request.getTutorId()).orElse(null);
        }
        if (tutor == null) {
            throw new IllegalArgumentException("Thiếu thông tin gia sư. Vui lòng chọn gia sư trước khi thanh toán.");
        }

        // 3. Resolve Student (Học sinh)
        Parent parent = null;
        Student student = null;
        if (currentUser != null && currentUser.getRole() == Role.STUDENT) {
            student = studentRepository.findById(currentUser.getId()).orElse(null);
            if (student == null) {
                throw new IllegalArgumentException("Thiếu thông tin học sinh. Vui lòng kiểm tra tài khoản học sinh của bạn.");
            }
        } else if (currentUser != null && currentUser.getRole() == Role.PARENT) {
            parent = parentRepository.findById(currentUser.getId()).orElse(null);
            if (parent == null) {
                throw new IllegalArgumentException("Thiếu thông tin phụ huynh. Vui lòng kiểm tra tài khoản phụ huynh của bạn.");
            }

            if (request.getStudentId() != null) {
                final Long currentParentId = parent.getId();
                student = studentRepository.findById(request.getStudentId())
                        .filter(s -> s.getParent() != null && currentParentId.equals(s.getParent().getId()))
                        .orElse(null);
            }
            if (student == null) {
                String studentName = request.getStudentName();
                String studentEmail = request.getStudentEmail();
                if ((studentName == null || studentName.isBlank()) && (studentEmail == null || studentEmail.isBlank()) && request.getStudentId() == null) {
                    throw new IllegalArgumentException("Thiếu thông tin học sinh. Vui lòng chọn học sinh hoặc nhập tên học sinh.");
                }
                if (studentEmail == null || studentEmail.isBlank()) {
                    studentEmail = "student." + System.currentTimeMillis() + "@giasuhq.com";
                }
                student = studentRepository.save(Student.builder()
                        .email(studentEmail.trim().toLowerCase())
                        .fullName(studentName != null && !studentName.isBlank() ? studentName : currentUser.getFullName())
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
            throw new IllegalArgumentException("Thiếu thông tin học sinh. Vui lòng chọn học sinh hợp lệ trước khi thanh toán.");
        }

        // 4. Resolve Parent (Phụ huynh)
        if (parent == null && currentUser != null && currentUser.getRole() == Role.PARENT) {
            parent = parentRepository.findById(currentUser.getId()).orElse(null);
            if (parent == null) {
                throw new IllegalArgumentException("Thiếu thông tin phụ huynh. Vui lòng đăng nhập lại.");
            }
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
                .status(ClassStatus.ACTIVE)
                .build();

        TutoringClass savedClass = tutoringClassRepository.save(newClass);

        // 5. Automatically create the first lesson in the lessons table
        LocalDateTime startTime = parseStartTime(request.getDate(), request.getTime());
        LocalDateTime endTime = startTime.plusHours(1);

        Lesson initialLesson = Lesson.builder()
                .tutoringClass(savedClass)
                .title("Buổi 1: " + savedClass.getClassName())
                .startTime(startTime)
                .endTime(endTime)
                .status(LessonStatus.SCHEDULED)
                .build();
        lessonRepository.save(initialLesson);

        log.info("Class created successfully with ID: {} and Lesson ID: {}", savedClass.getId(), initialLesson.getId());
        return mapToResponse(savedClass);
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
                .status(tc.getStatus())
                .createdAt(tc.getCreatedAt())
                .build();
    }
}
