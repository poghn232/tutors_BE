package com.giasuhq.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReviewRequest(
        @NotNull(message = "Số sao đánh giá không được để trống")
        @Min(value = 1, message = "Đánh giá tối thiểu là 1 sao")
        @Max(value = 5, message = "Đánh giá tối đa là 5 sao")
        Integer rating,

        @NotBlank(message = "Nội dung nhận xét không được để trống")
        String comment,

        String authorName
) {}
