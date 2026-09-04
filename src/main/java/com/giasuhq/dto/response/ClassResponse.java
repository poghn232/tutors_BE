package com.giasuhq.dto.response;

import com.giasuhq.entity.ClassStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassResponse {
    private Long id;
    private String className;
    private Long tutorId;
    private String tutorName;
    private Long studentId;
    private String studentName;
    private Long parentId;
    private String parentName;
    private Long subjectId;
    private String subjectName;
    private String scheduleDescription;
    private ClassStatus status;
    private LocalDateTime createdAt;
}
