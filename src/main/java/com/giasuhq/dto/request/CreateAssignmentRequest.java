package com.giasuhq.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAssignmentRequest {

    @NotBlank(message = "Tiêu đề bài tập không được để trống")
    private String title;

    private String description;

    @NotBlank(message = "Môn học không được để trống")
    private String subjectName;

    @NotNull(message = "Vui lòng chọn phụ huynh nhận bài tập")
    private Long parentId;

    private Long classId;

    @NotNull(message = "Hạn nộp bài tập không được để trống")
    private LocalDateTime dueDate;

    private String attachmentUrl;
    private String attachmentName;
    private String attachmentSize;
}
