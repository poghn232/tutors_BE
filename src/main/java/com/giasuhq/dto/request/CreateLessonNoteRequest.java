package com.giasuhq.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateLessonNoteRequest {
    @NotBlank(message = "Ghi chú thô của gia sư không được để trống")
    private String rawTutorNote;

    private String aiSummary;
    private String keyLearnings;
    private String areasForImprovement;
}
