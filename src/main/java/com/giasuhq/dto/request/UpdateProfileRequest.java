package com.giasuhq.dto.request;

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
    private String phone;
    private String avatarUrl;

    // Các trường dành riêng cho Gia sư
    private String bio;
    private String qualification;
    private Integer experienceYears;
    private Double hourlyRate;

    // Dành cho Phụ huynh
    private String address;
    private String emergencyContact;

    // Dành cho Học sinh
    private String gradeLevel;
    private String schoolName;
}
