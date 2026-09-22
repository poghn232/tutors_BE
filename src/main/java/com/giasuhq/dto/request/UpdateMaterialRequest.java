package com.giasuhq.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateMaterialRequest {

    @NotBlank(message = "Tiêu đề tài liệu không được để trống")
    private String title;

    private String description;

    @NotBlank(message = "Vui lòng chọn môn học")
    private String subjectName;

    private String materialType; // pdf, video, exercise, quiz

    private String typeBadge; // PDF, Video, Bài tập, Trắc nghiệm

    private String badgeExtra;

    private String authorName;

    private String fileUrl;

    private String fileName;

    private String fileSize;

    private Boolean isVip;

    private String btnText;

    private String btnColor;
}
