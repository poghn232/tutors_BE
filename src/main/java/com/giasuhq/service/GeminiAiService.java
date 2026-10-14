package com.giasuhq.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giasuhq.dto.response.GenerateAiNoteResponse;
import com.giasuhq.dto.response.ProgressAiReportResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class GeminiAiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.model:gemini-3.6-flash}")
    private String model;

    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models}")
    private String baseUrl;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public GeminiAiService() {
        this.restClient = RestClient.builder().build();
        this.objectMapper = new ObjectMapper();
    }

    public GenerateAiNoteResponse generateLessonNote(String subjectName, String studentName, String lessonTitle, String rawNote) {
        if (!hasUsableApiKey()) {
            log.warn("GEMINI_API_KEY chưa được cấu hình; sử dụng fallback cho AI Note.");
            return fallbackResponse(rawNote);
        }

        try {
            String prompt = buildPrompt(subjectName, studentName, lessonTitle, rawNote);
            String endpoint = String.format("%s/%s:generateContent", baseUrl, model);

            Map<String, Object> requestBody = new HashMap<>();
            
            Map<String, Object> part = Map.of("text", prompt);
            Map<String, Object> content = Map.of(
                    "role", "user",
                    "parts", Collections.singletonList(part)
            );
            requestBody.put("contents", Collections.singletonList(content));

            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("responseMimeType", "application/json");
            generationConfig.put("temperature", 0.7);
            generationConfig.put("maxOutputTokens", 1200);
            generationConfig.put("responseSchema", lessonNoteSchema());
            requestBody.put("generationConfig", generationConfig);

            log.info("Gửi yêu cầu tới Gemini API ({}) cho bài học: {}", model, lessonTitle);

            String rawResponse = restClient.post()
                    .uri(endpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("x-goog-api-key", apiKey)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            if (rawResponse != null && !rawResponse.isBlank()) {
                JsonNode rootNode = objectMapper.readTree(rawResponse);
                JsonNode candidatesNode = rootNode.path("candidates");
                if (candidatesNode.isArray() && !candidatesNode.isEmpty()) {
                    JsonNode textNode = candidatesNode.get(0).path("content").path("parts").get(0).path("text");
                    if (!textNode.isMissingNode()) {
                        String generatedJsonText = cleanJsonString(textNode.asText());
                        GenerateAiNoteResponse response = objectMapper.readValue(generatedJsonText, GenerateAiNoteResponse.class);
                        response.setAiGenerated(true);
                        return response;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Không thể gọi Gemini cho Lesson Note, chuyển sang fallback: {}", e.getMessage());
        }

        // Fallback tự động nếu có lỗi hoặc mất kết nối
        return fallbackResponse(rawNote);
    }

    public ProgressAiReportResponse generateProgressReport(
            String studentName,
            String subjectName,
            String progressContext
    ) {
        if (!hasUsableApiKey()) {
            log.warn("GEMINI_API_KEY chưa được cấu hình; sử dụng fallback cho báo cáo tiến độ.");
            return fallbackProgressResponse(studentName, progressContext);
        }

        try {
            String prompt = buildProgressPrompt(studentName, subjectName, progressContext);
            String endpoint = String.format("%s/%s:generateContent", baseUrl, model);
            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> part = Map.of("text", prompt);
            Map<String, Object> content = Map.of(
                    "role", "user",
                    "parts", Collections.singletonList(part)
            );
            requestBody.put("contents", Collections.singletonList(content));

            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("responseMimeType", "application/json");
            generationConfig.put("temperature", 0.5);
            generationConfig.put("maxOutputTokens", 1400);
            generationConfig.put("responseSchema", progressReportSchema());
            requestBody.put("generationConfig", generationConfig);

            String rawResponse = restClient.post()
                    .uri(endpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("x-goog-api-key", apiKey)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            String generatedJson = extractText(rawResponse);
            if (generatedJson != null && !generatedJson.isBlank()) {
                ProgressAiReportResponse response = objectMapper.readValue(cleanJsonString(generatedJson), ProgressAiReportResponse.class);
                response.setAiGenerated(true);
                return response;
            }
        } catch (Exception e) {
            log.warn("Không thể gọi Gemini cho báo cáo tiến độ, chuyển sang fallback: {}", e.getMessage());
        }

        return fallbackProgressResponse(studentName, progressContext);
    }

    private String buildPrompt(String subjectName, String studentName, String lessonTitle, String rawNote) {
        return """
                Bạn là một cố vấn sư phạm giàu kinh nghiệm tại nền tảng gia sư Gia Sư HQ.
                Nhiệm vụ: Chuyển đổi ghi chú thô của gia sư thành một bản báo cáo buổi học chuyên nghiệp, chu đáo, chuẩn mực sư phạm để gửi cho phụ huynh.

                Thông tin buổi học:
                - Môn học: %s
                - Học sinh: %s
                - Bài học: %s
                - Ghi chú thô của gia sư: "%s"

                Yêu cầu:
                - Giọng điệu: Lịch sự, ân cần, mang tính khích lệ học sinh nhưng trung thực, rõ ràng.
                - Xuất ra DUY NHẤT một JSON hợp lệ có đúng 3 trường:
                {
                  "aiSummary": "Tóm tắt ngắn gọn 2-3 câu về buổi học gửi phụ huynh.",
                  "keyLearnings": "Kiến thức và kỹ năng trọng tâm học sinh đã tiếp thu tốt trong buổi.",
                  "areasForImprovement": "Điểm cần lưu ý khắc phục kèm nhiệm vụ/bài tập về nhà cần làm."
                }
                """.formatted(
                subjectName != null ? subjectName : "Môn học",
                studentName != null ? studentName : "Học sinh",
                lessonTitle != null ? lessonTitle : "Buổi học",
                rawNote
        );
    }

    private boolean hasUsableApiKey() {
        if (apiKey == null) {
            return false;
        }
        String normalizedKey = apiKey.trim();
        return !normalizedKey.isBlank()
                && !normalizedKey.startsWith("${")
                && !normalizedKey.equalsIgnoreCase("your_gemini_api_key")
                && !normalizedKey.equalsIgnoreCase("changeme");
    }

    private String cleanJsonString(String raw) {
        String cleaned = raw.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        return cleaned.trim();
    }

    private String extractText(String rawResponse) throws Exception {
        if (rawResponse == null || rawResponse.isBlank()) {
            return null;
        }
        JsonNode rootNode = objectMapper.readTree(rawResponse);
        JsonNode candidatesNode = rootNode.path("candidates");
        if (!candidatesNode.isArray() || candidatesNode.isEmpty()) {
            return null;
        }
        JsonNode parts = candidatesNode.get(0).path("content").path("parts");
        if (!parts.isArray() || parts.isEmpty()) {
            return null;
        }
        JsonNode textNode = parts.get(0).path("text");
        return textNode.isMissingNode() ? null : textNode.asText();
    }

    private Map<String, Object> lessonNoteSchema() {
        return Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                        "aiSummary", Map.of("type", "STRING"),
                        "keyLearnings", Map.of("type", "STRING"),
                        "areasForImprovement", Map.of("type", "STRING")
                ),
                "required", List.of("aiSummary", "keyLearnings", "areasForImprovement")
        );
    }

    private Map<String, Object> progressReportSchema() {
        return Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                        "overallSummary", Map.of("type", "STRING"),
                        "strengths", Map.of("type", "STRING"),
                        "areasForImprovement", Map.of("type", "STRING"),
                        "nextSteps", Map.of("type", "STRING"),
                        "motivation", Map.of("type", "STRING")
                ),
                "required", List.of("overallSummary", "strengths", "areasForImprovement", "nextSteps", "motivation")
        );
    }

    private String buildProgressPrompt(String studentName, String subjectName, String progressContext) {
        return """
                Bạn là cố vấn học tập của nền tảng Gia Sư HQ.
                Hãy viết báo cáo tiến độ học tập dựa CHỈ trên dữ liệu được cung cấp, không tự bịa điểm số hoặc thành tích.

                Học sinh: %s
                Môn học/phạm vi: %s
                Dữ liệu thống kê và ghi chú:
                %s

                Yêu cầu:
                - Viết bằng tiếng Việt, rõ ràng, tích cực nhưng trung thực.
                - Phân biệt dữ liệu đã hoàn thành với lịch học sắp tới.
                - Đưa ra 2-3 việc cần làm tiếp theo có thể đo lường được.
                - Xuất DUY NHẤT JSON hợp lệ có đúng 5 trường:
                {
                  "overallSummary": "Đánh giá tổng quan tiến độ.",
                  "strengths": "Điểm mạnh và nội dung đang tiến bộ.",
                  "areasForImprovement": "Nội dung còn yếu hoặc rủi ro cần chú ý.",
                  "nextSteps": "Kế hoạch hành động cụ thể cho giai đoạn tới.",
                  "motivation": "Một lời động viên ngắn, phù hợp với học sinh."
                }
                """.formatted(
                studentName != null && !studentName.isBlank() ? studentName : "Học sinh",
                subjectName != null && !subjectName.isBlank() ? subjectName : "Tổng hợp",
                progressContext != null ? progressContext : "Chưa có dữ liệu."
        );
    }

    private GenerateAiNoteResponse fallbackResponse(String rawNote) {
        return GenerateAiNoteResponse.builder()
                .aiSummary("📌 [AI Note Tóm tắt]: Buổi học đã hoàn thành tốt. Gia sư đã giảng dạy các nội dung trọng tâm: " + rawNote)
                .keyLearnings("Nắm vững định lý, khái niệm cốt lõi và các dạng bài tập thực hành trong buổi học.")
                .areasForImprovement("Cần rèn luyện thêm bài tập tự luyện và chuẩn bị bài mới trước buổi học kế tiếp.")
                .aiGenerated(false)
                .build();
    }

    private ProgressAiReportResponse fallbackProgressResponse(String studentName, String progressContext) {
        return ProgressAiReportResponse.builder()
                .overallSummary((studentName != null ? studentName : "Học sinh")
                        + " đang có tiến độ học tập được ghi nhận qua các buổi học và bài tập trên hệ thống."
                        + " Hãy duy trì lịch học đều và hoàn thành các việc cần cải thiện trong báo cáo.")
                .strengths("Đã có dữ liệu buổi học, ghi chú của gia sư và bài tập để theo dõi tiến bộ.")
                .areasForImprovement("Tiếp tục cập nhật ghi chú sau mỗi buổi học và hoàn thành bài tập đúng hạn để báo cáo chính xác hơn.")
                .nextSteps("Ôn lại nội dung của buổi gần nhất; hoàn thành bài tập còn mở; đặt mục tiêu cụ thể cho buổi học kế tiếp.")
                .motivation("Mỗi buổi học đều là một bước tiến. Hãy giữ nhịp học đều đặn và hỏi ngay khi gặp phần chưa rõ.")
                .aiGenerated(false)
                .build();
    }
}
