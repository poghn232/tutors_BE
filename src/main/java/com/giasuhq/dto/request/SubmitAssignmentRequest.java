package com.giasuhq.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmitAssignmentRequest {

    @NotBlank(message = "Đường dẫn tệp bài làm không được để trống")
    private String submittedFileUrl;

    @NotBlank(message = "Tên tệp bài làm không được để trống")
    private String submittedFileName;

    private String submittedFileSize;

    private String submissionNote;
}
