package com.giasuhq.dto.response;

import com.giasuhq.entity.LessonStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonResponse {
    private Long id;
    private Long classId;
    private String className;
    private String subjectName;
    private String tutorName;
    private String studentName;
    private String parentName;
    private String title;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LessonStatus status;
    private LessonNoteResponse lessonNote;
    private LocalDateTime createdAt;
}
