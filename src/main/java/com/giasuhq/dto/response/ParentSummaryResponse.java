package com.giasuhq.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParentSummaryResponse {
    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private String studentName;
    private String studentGradeLevel;
    private String studentSchoolName;
    private String avatarUrl;
}
