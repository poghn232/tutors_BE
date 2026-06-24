package com.giasuhq.controller;

import com.giasuhq.dto.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public ApiResponse<String> healthCheck() {
        return ApiResponse.success("Gia Su HQ backend is running");
    }
}
