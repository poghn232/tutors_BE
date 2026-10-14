package com.giasuhq.dto.response;

import com.giasuhq.entity.LessonStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * A parent-safe view of the tutor's note for one lesson.
 * The raw internal note is intentionally omitted; the polished fields are
 * enough for progress reporting and avoid leaking scratch text to parents.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressLessonNoteResponse {
    private Long lessonId;
    private Long classId;
    private String className;
    private String title;
    private LocalDateTime startTime;
    private LessonStatus status;
    private String aiSummary;
    private String keyLearnings;
    private String areasForImprovement;
}
