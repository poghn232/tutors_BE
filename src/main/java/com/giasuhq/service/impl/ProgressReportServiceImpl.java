package com.giasuhq.service.impl;

import com.giasuhq.dto.response.ProgressAiReportResponse;
import com.giasuhq.dto.response.ProgressClassResponse;
import com.giasuhq.dto.response.ProgressReportResponse;
import com.giasuhq.entity.*;
import com.giasuhq.exception.ResourceNotFoundException;
import com.giasuhq.repository.AssignmentRepository;
import com.giasuhq.repository.LessonNoteRepository;
import com.giasuhq.repository.LessonRepository;
import com.giasuhq.repository.TutoringClassRepository;
import com.giasuhq.service.GeminiAiService;
import com.giasuhq.service.ProgressReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProgressReportServiceImpl implements ProgressReportService {

    private final TutoringClassRepository tutoringClassRepository;
    private final LessonRepository lessonRepository;
    private final LessonNoteRepository lessonNoteRepository;
    private final AssignmentRepository assignmentRepository;
    private final GeminiAiService geminiAiService;

    @Override
    @Transactional(readOnly = true)
    public ProgressReportResponse generateReport(User currentUser, Long classId) {
        if (currentUser == null || currentUser.getId() == null) {
            throw new IllegalArgumentException("Vui lòng đăng nhập để xem báo cáo tiến độ.");
        }

        List<TutoringClass> classes = loadClasses(currentUser);
        if (classId != null) {
            classes = classes.stream()
                    .filter(item -> classId.equals(item.getId()))
                    .collect(Collectors.toList());
            if (classes.isEmpty()) {
                throw new ResourceNotFoundException("Không tìm thấy lớp học hoặc bạn không có quyền xem lớp này.");
            }
        }

        List<Assignment> assignments = loadAssignments(currentUser);
        if (classId != null) {
            assignments = assignments.stream()
                    .filter(item -> item.getTutoringClass() != null && classId.equals(item.getTutoringClass().getId()))
                    .collect(Collectors.toList());
        }

        final List<Assignment> reportAssignments = assignments;
        List<ProgressClassResponse> classProgress = classes.stream()
                .map(item -> mapClassProgress(item, reportAssignments))
                .collect(Collectors.toList());

        int totalLessons = classProgress.stream().mapToInt(ProgressClassResponse::getTotalLessons).sum();
        int completedLessons = classProgress.stream().mapToInt(ProgressClassResponse::getCompletedLessons).sum();
        int upcomingLessons = classProgress.stream().mapToInt(ProgressClassResponse::getUpcomingLessons).sum();
        BigDecimal totalHours = classProgress.stream()
                .map(ProgressClassResponse::getTotalHours)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(1, RoundingMode.HALF_UP);

        int submittedAssignments = (int) assignments.stream()
                .filter(item -> item.getStatus() == AssignmentStatus.SUBMITTED)
                .count();
        int gradedAssignments = (int) assignments.stream()
                .filter(item -> item.getStatus() == AssignmentStatus.GRADED)
                .count();
        int overdueAssignments = (int) assignments.stream()
                .filter(this::isOverdue)
                .count();
        BigDecimal averageScore = averageScore(assignments);

        String studentName = resolveStudentName(currentUser, classes, classId);
        String subjectName = resolveSubjectName(classes, classId);
        String context = buildAiContext(classProgress, assignments);
        ProgressAiReportResponse aiReport = geminiAiService.generateProgressReport(studentName, subjectName, context);

        return ProgressReportResponse.builder()
                .generatedAt(LocalDateTime.now())
                .studentName(studentName)
                .subjectName(subjectName)
                .scopeLabel(classId == null ? "Tổng hợp các lớp đang theo dõi" : "Chi tiết lớp học được chọn")
                .totalClasses(classes.size())
                .totalLessons(totalLessons)
                .completedLessons(completedLessons)
                .upcomingLessons(upcomingLessons)
                .completionRate(rate(completedLessons, totalLessons))
                .totalHours(totalHours)
                .totalAssignments(assignments.size())
                .submittedAssignments(submittedAssignments)
                .gradedAssignments(gradedAssignments)
                .overdueAssignments(overdueAssignments)
                .averageAssignmentScore(averageScore)
                .classes(classProgress)
                .aiReport(aiReport)
                .build();
    }

    private List<TutoringClass> loadClasses(User currentUser) {
        if (currentUser.getRole() == Role.ADMIN) {
            return tutoringClassRepository.findAllByOrderByCreatedAtDesc();
        }
        if (currentUser.getRole() == Role.TUTOR) {
        return tutoringClassRepository.findByTutorIdOrderByCreatedAtDesc(currentUser.getId()).stream()
                .filter(this::isConnectedClass)
                .collect(Collectors.toList());
        }
        return tutoringClassRepository.findByParentIdOrderByCreatedAtDesc(currentUser.getId()).stream()
                .filter(this::isConnectedClass)
                .collect(Collectors.toList());
    }

    private boolean isConnectedClass(TutoringClass tutoringClass) {
        if (tutoringClass == null) {
            return false;
        }
        return tutoringClass.getPaidAt() != null
                || tutoringClass.getStatus() == ClassStatus.PENDING_PAYMENT
                || tutoringClass.getStatus() == ClassStatus.ACTIVE
                || tutoringClass.getStatus() == ClassStatus.PAUSED
                || tutoringClass.getStatus() == ClassStatus.COMPLETED;
    }

    private List<Assignment> loadAssignments(User currentUser) {
        if (currentUser.getRole() == Role.ADMIN) {
            return assignmentRepository.findAll();
        }
        if (currentUser.getRole() == Role.TUTOR) {
            return assignmentRepository.findByTutorIdOrderByCreatedAtDesc(currentUser.getId());
        }
        return assignmentRepository.findByParentIdOrderByCreatedAtDesc(currentUser.getId());
    }

    private ProgressClassResponse mapClassProgress(TutoringClass tutoringClass, List<Assignment> allAssignments) {
        List<Lesson> lessons = lessonRepository.findByTutoringClassIdOrderByStartTimeDesc(tutoringClass.getId());
        List<Assignment> assignments = allAssignments.stream()
                .filter(item -> item.getTutoringClass() != null
                        && tutoringClass.getId().equals(item.getTutoringClass().getId()))
                .collect(Collectors.toList());

        int completed = (int) lessons.stream().filter(item -> item.getStatus() == LessonStatus.COMPLETED).count();
        int upcoming = (int) lessons.stream().filter(item -> item.getStatus() == LessonStatus.SCHEDULED).count();
        BigDecimal hours = lessons.stream()
                .map(this::durationHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(1, RoundingMode.HALF_UP);
        List<String> recentTopics = lessons.stream()
                .sorted(Comparator.comparing(Lesson::getStartTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(3)
                .map(this::topic)
                .filter(item -> item != null && !item.isBlank())
                .collect(Collectors.toList());

        return ProgressClassResponse.builder()
                .classId(tutoringClass.getId())
                .className(tutoringClass.getClassName())
                .subjectName(tutoringClass.getSubject() != null ? tutoringClass.getSubject().getName() : "Môn học")
                .studentName(resolveClassStudentName(tutoringClass))
                .totalLessons(lessons.size())
                .completedLessons(completed)
                .upcomingLessons(upcoming)
                .completionRate(rate(completed, lessons.size()))
                .totalHours(hours)
                .totalAssignments(assignments.size())
                .gradedAssignments((int) assignments.stream().filter(item -> item.getStatus() == AssignmentStatus.GRADED).count())
                .overdueAssignments((int) assignments.stream().filter(this::isOverdue).count())
                .averageAssignmentScore(averageScore(assignments))
                .recentTopics(recentTopics)
                .build();
    }

    private String buildAiContext(List<ProgressClassResponse> classProgress, List<Assignment> assignments) {
        StringBuilder context = new StringBuilder();
        for (ProgressClassResponse progress : classProgress) {
            context.append("- Lớp ").append(progress.getClassName())
                    .append(" | môn ").append(progress.getSubjectName())
                    .append(" | học sinh ").append(progress.getStudentName())
                    .append(" | buổi hoàn thành ").append(progress.getCompletedLessons())
                    .append('/').append(progress.getTotalLessons())
                    .append(" | tỷ lệ ").append(progress.getCompletionRate()).append('%')
                    .append(" | giờ học ").append(progress.getTotalHours())
                    .append(" | bài tập ").append(progress.getGradedAssignments())
                    .append('/').append(progress.getTotalAssignments())
                    .append(" | điểm TB ").append(formatScore(progress.getAverageAssignmentScore()))
                    .append(" | chủ đề gần đây: ")
                    .append(String.join(", ", progress.getRecentTopics()))
                    .append('\n');
        }
        context.append("- Tổng bài tập: ").append(assignments.size())
                .append("; đã chấm: ")
                .append(assignments.stream().filter(item -> item.getStatus() == AssignmentStatus.GRADED).count())
                .append("; quá hạn: ")
                .append(assignments.stream().filter(this::isOverdue).count());
        return context.toString();
    }

    private String resolveStudentName(User currentUser, List<TutoringClass> classes, Long classId) {
        if (classId != null && !classes.isEmpty()) {
            return resolveClassStudentName(classes.get(0));
        }
        if (currentUser instanceof Parent parent && parent.getStudentName() != null && !parent.getStudentName().isBlank()) {
            return parent.getStudentName();
        }
        List<String> names = classes.stream()
                .map(this::resolveClassStudentName)
                .filter(item -> item != null && !item.isBlank())
                .distinct()
                .limit(3)
                .collect(Collectors.toList());
        return names.isEmpty() ? "Học sinh" : (names.size() == 1 ? names.get(0) : "Các học sinh: " + String.join(", ", names));
    }

    private String resolveSubjectName(List<TutoringClass> classes, Long classId) {
        if (classId != null && !classes.isEmpty() && classes.get(0).getSubject() != null) {
            return classes.get(0).getSubject().getName();
        }
        List<String> subjects = classes.stream()
                .map(item -> item.getSubject() != null ? item.getSubject().getName() : null)
                .filter(item -> item != null && !item.isBlank())
                .distinct()
                .limit(3)
                .collect(Collectors.toList());
        return subjects.isEmpty() ? "Tổng hợp" : String.join(", ", subjects);
    }

    private String resolveClassStudentName(TutoringClass tutoringClass) {
        if (tutoringClass.getStudentName() != null && !tutoringClass.getStudentName().isBlank()) {
            return tutoringClass.getStudentName();
        }
        return tutoringClass.getParent() != null && tutoringClass.getParent().getStudentName() != null
                ? tutoringClass.getParent().getStudentName()
                : "Học sinh";
    }

    private String topic(Lesson lesson) {
        String title = lesson.getTitle();
        String noteTopic = lessonNoteRepository.findByLessonId(lesson.getId())
                .map(LessonNote::getKeyLearnings)
                .orElse(null);
        if (noteTopic != null && !noteTopic.isBlank()) {
            return title + ": " + noteTopic;
        }
        return title;
    }

    private BigDecimal durationHours(Lesson lesson) {
        if (lesson.getStartTime() == null || lesson.getEndTime() == null) {
            return BigDecimal.ZERO;
        }
        long minutes = Math.max(0, Duration.between(lesson.getStartTime(), lesson.getEndTime()).toMinutes());
        return BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 1, RoundingMode.HALF_UP);
    }

    private BigDecimal rate(int completed, int total) {
        if (total <= 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(completed * 100L)
                .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
    }

    private BigDecimal averageScore(List<Assignment> assignments) {
        List<BigDecimal> scores = assignments.stream()
                .filter(item -> item.getStatus() == AssignmentStatus.GRADED && item.getRating() != null)
                .map(Assignment::getRating)
                .collect(Collectors.toList());
        if (scores.isEmpty()) {
            return null;
        }
        BigDecimal total = scores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(scores.size()), 1, RoundingMode.HALF_UP);
    }

    private boolean isOverdue(Assignment assignment) {
        if (assignment.getStatus() == AssignmentStatus.NOT_SUBMITTED) {
            return true;
        }
        return assignment.getStatus() == AssignmentStatus.PENDING
                && assignment.getDueDate() != null
                && assignment.getDueDate().isBefore(LocalDateTime.now());
    }

    private String formatScore(BigDecimal score) {
        return score == null ? "chưa có" : score.setScale(1, RoundingMode.HALF_UP).toPlainString() + "/10";
    }
}
