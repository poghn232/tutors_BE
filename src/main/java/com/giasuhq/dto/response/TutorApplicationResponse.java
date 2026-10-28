package com.giasuhq.dto.response;

import com.giasuhq.entity.TutorApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TutorApplicationResponse {
    private Long id;
    private Long postId;
    private String postTitle;

    private Long tutorId;
    private String tutorName;
    private String tutorEmail;
    private String tutorPhone;
    private String tutorAvatar;
    private String tutorBio;
    private String tutorQualification;
    private Integer tutorExperienceYears;
    private Double tutorRating;

    private String coverLetter;
    private BigDecimal proposedRate;
    private TutorApplicationStatus status;
    private String rejectionNote;

    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;

    private Long tutoringClassId;
}
