package com.giasuhq.mapper;

import com.giasuhq.dto.request.CreateTutorRequest;
import com.giasuhq.dto.response.TutorResponse;
import com.giasuhq.entity.Role;
import com.giasuhq.entity.Subject;
import com.giasuhq.entity.Tutor;
import com.giasuhq.util.ContactUtils;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class TutorMapper {

    public Tutor toEntity(Long id, CreateTutorRequest request) {
        return Tutor.builder()
                .id(id)
                .fullName(request.fullName())
                .email(request.email())
                .phone(ContactUtils.normalizePhone(request.phone()))
                .facebookUrl(request.facebookUrl())
                .password("default_password")
                .role(Role.TUTOR)
                .build();
    }

    public TutorResponse toResponse(Tutor tutor) {
        return toResponse(tutor, 0.0, 0);
    }

    public TutorResponse toResponse(Tutor tutor, Double rating, Integer reviewsCount) {
        List<String> subjectNames = tutor.getSubjects() != null
                ? tutor.getSubjects().stream().map(Subject::getName).toList()
                : Collections.emptyList();

        double safeRating = rating != null ? Math.round(rating * 10.0) / 10.0 : 0.0;
        int safeCount = reviewsCount != null ? reviewsCount : 0;

        return new TutorResponse(
                tutor.getId(),
                tutor.getFullName(),
                tutor.getQualification() != null ? tutor.getQualification() : "Chưa cập nhật",
                tutor.getEmail(),
                tutor.getPhone() != null ? tutor.getPhone() : "",
                tutor.getAvatarUrl(),
                tutor.getFacebookUrl() != null ? tutor.getFacebookUrl() : "",
                tutor.getBio() != null ? tutor.getBio() : "",
                tutor.getExperienceYears() != null ? tutor.getExperienceYears() : 0,
                tutor.getVerificationStatus() != null ? tutor.getVerificationStatus() : "PENDING",
                tutor.getRejectionReason(),
                tutor.getVerifiedAt(),
                tutor.getCertificatesJson(),
                tutor.getAvailableDaysJson(),
                tutor.getAvailableTimesJson(),
                tutor.getCreatedAt(),
                subjectNames,
                safeRating,
                safeCount
        );
    }
}
