package com.giasuhq.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GradeAssignmentRequest {

    @NotNull(message = "Điểm số rating không được để trống")
    @DecimalMin(value = "0.0", message = "Điểm số rating tối thiểu là 0.0")
    @DecimalMax(value = "10.0", message = "Điểm số rating tối đa là 10.0")
    private BigDecimal rating;

    @NotBlank(message = "Vui lòng nhập nhận xét bài tập")
    private String tutorComment;
}
