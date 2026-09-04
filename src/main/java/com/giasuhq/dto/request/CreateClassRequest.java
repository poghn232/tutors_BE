package com.giasuhq.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateClassRequest {

    @NotBlank(message = "Tên lớp học không được để trống")
    private String className;

    @NotNull(message = "ID môn học không được để trống")
    private Long subjectId;

    private Long tutorId;
    private Long studentId;
    private Long parentId;

    private String studentName;
    private String studentEmail;
    private String scheduleDescription;
}
