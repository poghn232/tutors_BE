package com.giasuhq.service.impl;

import com.giasuhq.dto.request.CreateLessonNoteRequest;
import com.giasuhq.dto.request.CreateLessonRequest;
import com.giasuhq.dto.response.LessonNoteResponse;
import com.giasuhq.dto.response.LessonResponse;
import com.giasuhq.entity.*;
import com.giasuhq.exception.ResourceNotFoundException;
import com.giasuhq.repository.*;
import com.giasuhq.service.LessonService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.giasuhq.dto.request.GenerateAiNoteRequest;
import com.giasuhq.dto.response.GenerateAiNoteResponse;
import com.giasuhq.service.GeminiAiService;

@Service
@RequiredArgsConstructor
public class LessonServiceImpl implements LessonService {

    private final LessonRepository lessonRepository;
    private final LessonNoteRepository lessonNoteRepository;
    private final TutoringClassRepository tutoringClassRepository;
    private final SubjectRepository subjectRepository;
    private final GeminiAiService geminiAiService;
    private final TutorRepository tutorRepository;
    private final ParentRepository parentRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public LessonResponse createLesson(CreateLessonRequest request, User currentUser) {
        TutoringClass tutoringClass = tutoringClassRepository.findById(request.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học với ID: " + request.getClassId()));

        Lesson lesson = Lesson.builder()
                .tutoringClass(tutoringClass)
                .title(request.getTitle())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status(LessonStatus.SCHEDULED)
                .build();

        Lesson savedLesson = lessonRepository.save(lesson);
        return mapToLessonResponse(savedLesson);
    }

    @Override
    @Transactional
    public List<LessonResponse> getLessonsForUser(User currentUser) {
        List<Lesson> lessons;
        Role role = currentUser.getRole();

        if (role == Role.ADMIN) {
            lessons = lessonRepository.findAllByOrderByStartTimeDesc();
        } else if (role == Role.TUTOR) {
            lessons = lessonRepository.findByTutoringClass_Tutor_IdOrderByStartTimeDesc(currentUser.getId());
        } else {
            lessons = lessonRepository.findByTutoringClass_Parent_IdOrderByStartTimeDesc(currentUser.getId());
        }

        // Nếu người dùng vừa đăng ký và chưa có lớp/buổi học mẫu, tự động sinh 1 buổi học demo phù hợp vai trò
        if (lessons.isEmpty()) {
            lessons = bootstrapDemoLessonForUser(currentUser);
        }

        return lessons.stream()
                .map(this::mapToLessonResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public LessonResponse updateLessonStatus(Long lessonId, LessonStatus status, User currentUser) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy buổi học với ID: " + lessonId));

        lesson.setStatus(status);
        Lesson updatedLesson = lessonRepository.save(lesson);
        return mapToLessonResponse(updatedLesson);
    }

    @Override
    @Transactional
    public LessonNoteResponse addOrUpdateLessonNote(Long lessonId, CreateLessonNoteRequest request, User currentUser) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy buổi học với ID: " + lessonId));

        Optional<LessonNote> noteOptional = lessonNoteRepository.findByLessonId(lessonId);
        LessonNote note;

        String rawNote = request.getRawTutorNote() != null ? request.getRawTutorNote() : "";
        String aiSummary = request.getAiSummary();
        if (aiSummary == null || aiSummary.isBlank()) {
            aiSummary = "📌 [Ghi chú buổi học]: " + rawNote;
        }

        if (noteOptional.isPresent()) {
            note = noteOptional.get();
            note.setRawTutorNote(rawNote);
            note.setAiSummary(aiSummary);
            note.setKeyLearnings(request.getKeyLearnings() != null ? request.getKeyLearnings() : "Kiến thức cốt lõi của bài học");
            note.setAreasForImprovement(request.getAreasForImprovement() != null ? request.getAreasForImprovement() : "Cần rèn luyện thêm bài tập tự luyện");
        } else {
            note = LessonNote.builder()
                    .lesson(lesson)
                    .rawTutorNote(rawNote)
                    .aiSummary(aiSummary)
                    .keyLearnings(request.getKeyLearnings() != null ? request.getKeyLearnings() : "Kiến thức cốt lõi của bài học")
                    .areasForImprovement(request.getAreasForImprovement() != null ? request.getAreasForImprovement() : "Cần rèn luyện thêm bài tập tự luyện")
                    .build();
        }

        LessonNote savedNote = lessonNoteRepository.save(note);
        
        // Cập nhật trạng thái buổi học thành COMPLETED nếu đang ở SCHEDULED
        if (lesson.getStatus() == LessonStatus.SCHEDULED) {
            lesson.setStatus(LessonStatus.COMPLETED);
            lessonRepository.save(lesson);
        }

        return mapToLessonNoteResponse(savedNote);
    }

    @Override
    @Transactional(readOnly = true)
    public GenerateAiNoteResponse generateAiLessonNote(Long lessonId, GenerateAiNoteRequest request, User currentUser) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy buổi học với ID: " + lessonId));

        TutoringClass tutoringClass = lesson.getTutoringClass();
        String subjectName = (tutoringClass != null && tutoringClass.getSubject() != null) 
                ? tutoringClass.getSubject().getName() 
                : "Môn học";
        String studentName = (tutoringClass != null && tutoringClass.getStudentName() != null)
                ? tutoringClass.getStudentName()
                : (tutoringClass != null && tutoringClass.getParent() != null ? tutoringClass.getParent().getStudentName() : "Học sinh");
        String lessonTitle = lesson.getTitle() != null ? lesson.getTitle() : "Buổi học";

        return geminiAiService.generateLessonNote(subjectName, studentName, lessonTitle, request.getRawNote());
    }

    private List<Lesson> bootstrapDemoLessonForUser(User currentUser) {
        Subject subject = subjectRepository.findAll().stream().findFirst().orElseGet(() -> 
            subjectRepository.save(Subject.builder().code("MATH").name("Toán Học").description("Môn Toán THPT").build())
        );

        Tutor tutor;
        if (currentUser != null && currentUser.getRole() == Role.TUTOR) {
            tutor = tutorRepository.findById(currentUser.getId()).orElse(null);
        } else {
            tutor = tutorRepository.findAll().stream().findFirst().orElse(null);
        }
        if (tutor == null) {
            User baseUser = userRepository.save(User.builder()
                    .email("giasumau@giasuhq.com")
                    .fullName("TS. Hoàng Thiên Ứng")
                    .password("$2a$10$10Q2J.X5iX/KOM4nHtFMfeXi4JoW3O6sv4ZtaJ6Ab2P0FNC71XcpO")
                    .role(Role.TUTOR)
                    .build());
            tutor = tutorRepository.save(Tutor.builder()
                    .id(baseUser.getId())
                    .email(baseUser.getEmail())
                    .fullName(baseUser.getFullName())
                    .password(baseUser.getPassword())
                    .role(Role.TUTOR)
                    .qualification("Tiến sĩ Toán học")
                    .experienceYears(8)
                    .hourlyRate(250000.0)
                    .build());
        }

        Parent parent = null;
        if (currentUser != null && currentUser.getRole() == Role.PARENT) {
            parent = parentRepository.findById(currentUser.getId()).orElse(null);
        }
        if (parent == null) {
            parent = parentRepository.findAll().stream().findFirst().orElse(null);
        }

        String studentName = parent != null && parent.getStudentName() != null && !parent.getStudentName().isBlank()
                ? parent.getStudentName()
                : "Học sinh Mẫu";


        TutoringClass demoClass = TutoringClass.builder()
                .className("Lớp Toán 12 - Ôn thi ĐHQG")
                .tutor(tutor)
                .studentName(studentName)
                .studentGradeLevel(parent != null ? parent.getStudentGradeLevel() : "Lớp 12")
                .studentSchoolName(parent != null ? parent.getStudentSchoolName() : "Trường Mẫu")
                .parent(parent)
                .subject(subject)
                .scheduleDescription("Thứ 3 và Thứ 5 (19:00 - 21:00)")
                .status(ClassStatus.ACTIVE)
                .build();
        demoClass = tutoringClassRepository.save(demoClass);

        Lesson demoLesson = Lesson.builder()
                .tutoringClass(demoClass)
                .title("Buổi 1: Hàm số và Đạo hàm nâng cao")
                .startTime(LocalDateTime.now().minusHours(2))
                .endTime(LocalDateTime.now().minusMinutes(30))
                .status(LessonStatus.COMPLETED)
                .build();
        demoLesson = lessonRepository.save(demoLesson);

        LessonNote demoNote = LessonNote.builder()
                .lesson(demoLesson)
                .rawTutorNote("Đã dạy xong phần cực trị hàm số hợp. Học sinh tiếp thu bài nhanh, làm tốt 8/10 bài tập tại lớp.")
                .aiSummary("📌 [AI Note Tóm tắt]: Học sinh nắm vững lý thuyết Cực trị hàm số. Tỷ lệ hoàn thành bài tập tại lớp đạt 80%.")
                .keyLearnings("Khái niệm đạo hàm cấp 1, cực đại/cực tiểu và ứng dụng xét biến thiên.")
                .areasForImprovement("Cần tính toán cẩn thận hơn ở các câu hỏi trắc nghiệm đếm số điểm cực trị.")
                .build();
        lessonNoteRepository.save(demoNote);

        List<Lesson> result = new ArrayList<>();
        result.add(demoLesson);
        return result;
    }

    private LessonResponse mapToLessonResponse(Lesson lesson) {
        TutoringClass tc = lesson.getTutoringClass();
        LessonNote note = lessonNoteRepository.findByLessonId(lesson.getId()).orElse(null);

        return LessonResponse.builder()
                .id(lesson.getId())
                .classId(tc.getId())
                .className(tc.getClassName())
                .subjectName(tc.getSubject() != null ? tc.getSubject().getName() : "Môn học")
                .tutorName(tc.getTutor() != null ? tc.getTutor().getFullName() : "Gia sư")
                .studentName(tc.getStudentName() != null ? tc.getStudentName() : (tc.getParent() != null ? tc.getParent().getStudentName() : "Học sinh"))
                .parentName(tc.getParent() != null ? tc.getParent().getFullName() : "Phụ huynh")
                .title(lesson.getTitle())
                .startTime(lesson.getStartTime())
                .endTime(lesson.getEndTime())
                .status(lesson.getStatus())
                .lessonNote(note != null ? mapToLessonNoteResponse(note) : null)
                .createdAt(lesson.getCreatedAt())
                .build();
    }

    private LessonNoteResponse mapToLessonNoteResponse(LessonNote note) {
        return LessonNoteResponse.builder()
                .id(note.getId())
                .lessonId(note.getLesson().getId())
                .rawTutorNote(note.getRawTutorNote())
                .aiSummary(note.getAiSummary())
                .keyLearnings(note.getKeyLearnings())
                .areasForImprovement(note.getAreasForImprovement())
                .createdAt(note.getCreatedAt())
                .updatedAt(note.getUpdatedAt())
                .build();
    }
}
