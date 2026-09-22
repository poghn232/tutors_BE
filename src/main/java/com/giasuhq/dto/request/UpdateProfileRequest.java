package com.giasuhq.dto.request;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {
    private String fullName;
    @Pattern(regexp = "^$|0[35789]\\d{8}", message = "Số điện thoại phải là số Việt Nam 10 chữ số")
    private String phone;
    private String avatarUrl;

    // Các trường dành riêng cho Gia sư
    private String bio;
    private String qualification;
    private Integer experienceYears;
    private String certificatesJson;

    // Dành cho Phụ huynh
    private String address;
    @Pattern(regexp = "^$|0[35789]\\d{8}", message = "Số điện thoại phải là số Việt Nam 10 chữ số")
    private String emergencyContact;
    private String studentName;
    private String gradeLevel;
    private String schoolName;
}
