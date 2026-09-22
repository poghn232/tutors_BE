package com.giasuhq.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateMaterialRequest {

    @NotBlank(message = "Tiêu đề tài liệu không được để trống")
    private String title;

    private String description;

    @NotBlank(message = "Vui lòng chọn môn học")
    private String subjectName;

    private String materialType; // pdf, video, exercise, quiz

    private String typeBadge; // PDF, Video, Bài tập, Trắc nghiệm

    private String badgeExtra; // MỚI, HOT...

    private String authorName;

    private String fileUrl;

    private String fileName;

    private String fileSize;

    private Boolean isVip; // Checkbox VIP (true = Chỉ dành cho phụ huynh VIP)

    private String btnText;

    private String btnColor;
}
