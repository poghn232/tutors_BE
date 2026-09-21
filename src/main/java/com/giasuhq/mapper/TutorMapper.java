package com.giasuhq.mapper;

import com.giasuhq.dto.request.CreateTutorRequest;
import com.giasuhq.dto.response.TutorResponse;
import com.giasuhq.entity.Role;
import com.giasuhq.entity.Tutor;
import org.springframework.stereotype.Component;

@Component
public class TutorMapper {

    public Tutor toEntity(Long id, CreateTutorRequest request) {
        return Tutor.builder()
                .id(id)
                .fullName(request.fullName())
                .email(request.email())
                .phone(request.phone())
                .facebookUrl(request.facebookUrl())
                .password("default_password")
                .role(Role.TUTOR)
                .hourlyRate(request.hourlyRate())
                .build();
    }

    public TutorResponse toResponse(Tutor tutor) {
        return new TutorResponse(
                tutor.getId(),
                tutor.getFullName(),
                tutor.getQualification() != null ? tutor.getQualification() : "Chưa cập nhật",
                tutor.getEmail(),
                tutor.getPhone() != null ? tutor.getPhone() : "",
                tutor.getFacebookUrl() != null ? tutor.getFacebookUrl() : "",
                tutor.getBio() != null ? tutor.getBio() : "",
                tutor.getHourlyRate() != null ? tutor.getHourlyRate() : 0.0,
                tutor.getExperienceYears() != null ? tutor.getExperienceYears() : 0
        );
    }
}
