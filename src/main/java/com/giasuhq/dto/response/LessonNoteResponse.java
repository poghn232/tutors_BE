package com.giasuhq.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonNoteResponse {
    private Long id;
    private Long lessonId;
    private String rawTutorNote;
    private String aiSummary;
    private String keyLearnings;
    private String areasForImprovement;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
