package com.giasuhq.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressAiReportResponse {
    private String overallSummary;
    private String strengths;
    private String areasForImprovement;
    private String nextSteps;
    private String motivation;
}
