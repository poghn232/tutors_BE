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
    private final GeminiAiService geminiAiService;

    @Override
    @Transactional
    public LessonResponse createLesson(CreateLessonRequest request, User currentUser) {
        TutoringClass tutoringClass = tutoringClassRepository.findById(request.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học với ID: " + request.getClassId()));
        ensureCanManageLesson(tutoringClass, currentUser);

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

        return lessons.stream()
                .map(this::mapToLessonResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public LessonResponse updateLessonStatus(Long lessonId, LessonStatus status, User currentUser) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy buổi học với ID: " + lessonId));
        ensureCanManageLesson(lesson.getTutoringClass(), currentUser);

        lesson.setStatus(status);
        Lesson updatedLesson = lessonRepository.save(lesson);
        return mapToLessonResponse(updatedLesson);
    }

    @Override
    @Transactional
    public LessonNoteResponse addOrUpdateLessonNote(Long lessonId, CreateLessonNoteRequest request, User currentUser) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy buổi học với ID: " + lessonId));
        ensureCanManageLesson(lesson.getTutoringClass(), currentUser);

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
        ensureCanManageLesson(lesson.getTutoringClass(), currentUser);

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

    private void ensureCanManageLesson(TutoringClass tutoringClass, User currentUser) {
        if (currentUser == null) {
            throw new IllegalArgumentException("Người dùng chưa đăng nhập hoặc phiên làm việc đã hết hạn.");
        }
        if (currentUser.getRole() == Role.ADMIN) {
            return;
        }
        if (currentUser.getRole() != Role.TUTOR
                || tutoringClass == null
                || tutoringClass.getTutor() == null
                || !currentUser.getId().equals(tutoringClass.getTutor().getId())) {
            throw new IllegalArgumentException("Chỉ gia sư sở hữu lớp học mới có quyền quản lý buổi học này.");
        }
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
