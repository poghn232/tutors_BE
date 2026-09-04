package com.giasuhq.dto.response;

import com.giasuhq.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private Long id;
    private String email;
    private String fullName;
    private String phone;
    private String avatarUrl;
    private Role role;
    private LocalDateTime createdAt;

    // Chi tiết Gia sư
    private String bio;
    private String qualification;
    private Integer experienceYears;
    private Double hourlyRate;

    // Chi tiết Phụ huynh
    private String address;
    private String emergencyContact;

    // Chi tiết Học sinh
    private String gradeLevel;
    private String schoolName;
}
