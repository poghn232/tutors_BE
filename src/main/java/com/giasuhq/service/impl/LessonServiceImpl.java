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

@Service
@RequiredArgsConstructor
public class LessonServiceImpl implements LessonService {

    private final LessonRepository lessonRepository;
    private final LessonNoteRepository lessonNoteRepository;
    private final TutoringClassRepository tutoringClassRepository;
    private final SubjectRepository subjectRepository;

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

        if (role == Role.TUTOR) {
            lessons = lessonRepository.findByTutoringClass_Tutor_IdOrderByStartTimeDesc(currentUser.getId());
        } else if (role == Role.PARENT) {
            lessons = lessonRepository.findByTutoringClass_Parent_IdOrderByStartTimeDesc(currentUser.getId());
        } else if (role == Role.STUDENT) {
            lessons = lessonRepository.findByTutoringClass_Student_IdOrderByStartTimeDesc(currentUser.getId());
        } else {
            lessons = lessonRepository.findAll();
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

        String rawNote = request.getRawTutorNote();
        String aiSummary = request.getAiSummary();
        if (aiSummary == null || aiSummary.isBlank()) {
            aiSummary = "📌 [AI Note Tóm tắt]: Buổi học diễn ra tốt. Gia sư đã giảng dạy các nội dung chính: " + rawNote;
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

    private List<Lesson> bootstrapDemoLessonForUser(User currentUser) {
        Subject subject = subjectRepository.findAll().stream().findFirst().orElseGet(() -> 
            subjectRepository.save(Subject.builder().code("MATH").name("Toán học").description("Môn Toán THPT").build())
        );

        Tutor tutor = (currentUser instanceof Tutor) ? (Tutor) currentUser : 
            Tutor.builder().email("giasumau@giasuhq.com").fullName("Gia sư Nguyễn Văn Minh").password("123456").role(Role.TUTOR).build();
        
        Student student = (currentUser instanceof Student) ? (Student) currentUser : 
            Student.builder().email("hocsinhmau@giasuhq.com").fullName("Học sinh Trần Bảo Nam").password("123456").role(Role.STUDENT).gradeLevel("Lớp 12").build();

        Parent parent = (currentUser instanceof Parent) ? (Parent) currentUser : 
            Parent.builder().email("phuhuynhmau@giasuhq.com").fullName("Phụ huynh Trần Đức Anh").password("123456").role(Role.PARENT).build();

        TutoringClass demoClass = TutoringClass.builder()
                .className("Lớp Toán 12 - Ôn thi ĐHQG")
                .tutor(tutor)
                .student(student)
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
                .studentName(tc.getStudent() != null ? tc.getStudent().getFullName() : "Học sinh")
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
