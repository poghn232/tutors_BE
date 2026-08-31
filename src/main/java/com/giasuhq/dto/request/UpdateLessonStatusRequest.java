package com.giasuhq.dto.request;

import com.giasuhq.entity.LessonStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateLessonStatusRequest {
    @NotNull(message = "Trạng thái không được để trống")
    private LessonStatus status;
}
