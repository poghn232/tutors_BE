package com.giasuhq.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressReportResponse {
    private LocalDateTime generatedAt;
    private String studentName;
    private String subjectName;
    private String scopeLabel;
    private int totalClasses;
    private int totalLessons;
    private int completedLessons;
    private int upcomingLessons;
    private BigDecimal completionRate;
    private BigDecimal totalHours;
    private int totalAssignments;
    private int submittedAssignments;
    private int gradedAssignments;
    private int overdueAssignments;
    private BigDecimal averageAssignmentScore;
    private List<ProgressClassResponse> classes;
    private List<ProgressLessonNoteResponse> lessonNotes;
    private ProgressAiReportResponse aiReport;
}
