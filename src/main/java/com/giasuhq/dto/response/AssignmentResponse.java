package com.giasuhq.dto.response;

import com.giasuhq.entity.AssignmentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentResponse {
    private Long id;
    private String title;
    private String description;
    private String subjectName;
    
    private Long tutorId;
    private String tutorName;
    private String tutorEmail;

    private Long parentId;
    private String parentName;
    private String parentEmail;
    private String studentName;

    private Long classId;
    private String className;

    private String attachmentUrl;
    private String attachmentName;
    private String attachmentSize;

    private LocalDateTime dueDate;
    private AssignmentStatus status;
    private String statusLabel;

    private String submittedFileUrl;
    private String submittedFileName;
    private String submittedFileSize;
    private LocalDateTime submittedAt;
    private String submissionNote;

    private BigDecimal rating; // 0.0 - 10.0
    private String tutorComment;
    private LocalDateTime gradedAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isOverdue;
}
