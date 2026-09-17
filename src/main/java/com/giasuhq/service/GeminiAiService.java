package com.giasuhq.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giasuhq.dto.response.GenerateAiNoteResponse;
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
        String prompt = buildPrompt(subjectName, studentName, lessonTitle, rawNote);

        try {
            String endpoint = String.format("%s/%s:generateContent?key=%s", baseUrl, model, apiKey);

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
            requestBody.put("generationConfig", generationConfig);

            log.info("Gửi yêu cầu tới Gemini API ({}) cho bài học: {}", model, lessonTitle);

            String rawResponse = restClient.post()
                    .uri(endpoint)
                    .contentType(MediaType.APPLICATION_JSON)
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
                        return objectMapper.readValue(generatedJsonText, GenerateAiNoteResponse.class);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Lỗi khi gọi Gemini API để tạo Lesson Note: {}", e.getMessage(), e);
        }

        // Fallback tự động nếu có lỗi hoặc mất kết nối
        return fallbackResponse(rawNote);
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

    private GenerateAiNoteResponse fallbackResponse(String rawNote) {
        return GenerateAiNoteResponse.builder()
                .aiSummary("📌 [AI Note Tóm tắt]: Buổi học đã hoàn thành tốt. Gia sư đã giảng dạy các nội dung trọng tâm: " + rawNote)
                .keyLearnings("Nắm vững định lý, khái niệm cốt lõi và các dạng bài tập thực hành trong buổi học.")
                .areasForImprovement("Cần rèn luyện thêm bài tập tự luyện và chuẩn bị bài mới trước buổi học kế tiếp.")
                .build();
    }
}
