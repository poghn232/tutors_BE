package com.giasuhq.dto.response;

import com.giasuhq.entity.BudgetType;
import com.giasuhq.entity.TeachingMode;
import com.giasuhq.entity.TutorGenderPreference;
import com.giasuhq.entity.TutorPostStatus;
import com.giasuhq.entity.TutorTypePreference;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TutorPostResponse {
    private Long id;
    private Long creatorId;
    private String creatorName;
    private String creatorAvatar;
    private String creatorPhone;
    private String creatorEmail;

    private String title;
    private Long subjectId;
    private String subjectName;
    private String subjectCode;

    private String gradeLevel;
    private TeachingMode teachingMode;
    private String city;
    private String district;
    private String detailedAddress;

    private Integer sessionsPerWeek;
    private String preferredSchedule;

    private BigDecimal budgetAmount;
    private BudgetType budgetType;

    private TutorGenderPreference tutorGenderPreference;
    private TutorTypePreference tutorTypePreference;

    private String studentNotes;
    private String requirements;

    private TutorPostStatus status;
    private String rejectionReason;

    private Long applicationsCount;
    private Boolean hasApplied;
    private Long myApplicationId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
