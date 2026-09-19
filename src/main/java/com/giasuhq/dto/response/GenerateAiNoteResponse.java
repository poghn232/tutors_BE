package com.giasuhq.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateAiNoteResponse {
    private String aiSummary;
    private String keyLearnings;
    private String areasForImprovement;
}
