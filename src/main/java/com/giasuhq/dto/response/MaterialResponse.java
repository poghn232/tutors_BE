package com.giasuhq.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaterialResponse {

    private Long id;
    private String title;
    private String description;
    private String desc; // Alias for frontend compatibility
    private String subjectName;
    private String subject; // Alias for frontend compatibility
    private String materialType; // pdf, video, exercise, quiz
    private String type; // Alias for frontend compatibility
    private String typeBadge;
    private String badgeExtra;
    private String authorName;
    private String author; // Alias for frontend compatibility
    private String fileUrl;
    private String fileName;
    private String fileSize;
    private Boolean isVip;
    private Integer downloadsCount;
    private String downloads; // Formatted downloads e.g. "2,341"
    private String date; // Formatted date e.g. "02/09/2026"
    private String btnText;
    private String btnColor;
    private Long createdById;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
