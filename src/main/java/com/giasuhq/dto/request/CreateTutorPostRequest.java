package com.giasuhq.dto.request;

import com.giasuhq.entity.BudgetType;
import com.giasuhq.entity.TeachingMode;
import com.giasuhq.entity.TutorGenderPreference;
import com.giasuhq.entity.TutorTypePreference;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTutorPostRequest {

    @NotBlank(message = "Tiêu đề bài đăng không được để trống")
    private String title;

    @NotNull(message = "Vui lòng chọn môn học")
    private Long subjectId;

    @NotBlank(message = "Vui lòng chọn khối lớp học")
    private String gradeLevel;

    @NotNull(message = "Vui lòng chọn hình thức học")
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
}
