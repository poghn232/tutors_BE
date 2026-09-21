package com.giasuhq.dto.request;

import jakarta.validation.constraints.NotBlank;
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

    private Long subjectId;
    private String subjectName;

    private Long tutorId;
    private String tutorName;

    private Long studentId;
    private Long parentId;

    private String studentName;
    private String studentEmail;
    private String scheduleDescription;

    private String date;
    private String time;
    private Double amount;
    private String orderCode;
    private String paymentMethod;
    private String status;
}
