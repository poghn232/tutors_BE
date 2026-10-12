package com.giasuhq.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressClassResponse {
    private Long classId;
    private String className;
    private String subjectName;
    private String studentName;
    private int totalLessons;
    private int completedLessons;
    private int upcomingLessons;
    private BigDecimal completionRate;
    private BigDecimal totalHours;
    private int totalAssignments;
    private int gradedAssignments;
    private int overdueAssignments;
    private BigDecimal averageAssignmentScore;
    private List<String> recentTopics;
}
