package com.giasuhq.service.impl;

import com.giasuhq.dto.request.UpdateProfileRequest;
import com.giasuhq.dto.response.UserProfileResponse;
import com.giasuhq.entity.*;
import com.giasuhq.repository.*;
import com.giasuhq.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final TutorRepository tutorRepository;

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(User currentUser) {
        return mapToProfileResponse(currentUser);
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(User currentUser, UpdateProfileRequest request) {
        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            currentUser.setFullName(request.getFullName());
        }
        if (request.getPhone() != null) {
            currentUser.setPhone(request.getPhone());
        }
        if (request.getAvatarUrl() != null) {
            currentUser.setAvatarUrl(request.getAvatarUrl());
        }

        if (currentUser instanceof Tutor) {
            Tutor tutor = (Tutor) currentUser;
            if (request.getBio() != null) tutor.setBio(request.getBio());
            if (request.getQualification() != null) tutor.setQualification(request.getQualification());
            if (request.getExperienceYears() != null) tutor.setExperienceYears(request.getExperienceYears());
            if (request.getHourlyRate() != null) tutor.setHourlyRate(request.getHourlyRate());
        } else if (currentUser instanceof Parent) {
            Parent parent = (Parent) currentUser;
            if (request.getAddress() != null) parent.setAddress(request.getAddress());
            if (request.getEmergencyContact() != null) parent.setEmergencyContact(request.getEmergencyContact());
        } else if (currentUser instanceof Student) {
            Student student = (Student) currentUser;
            if (request.getGradeLevel() != null) student.setGradeLevel(request.getGradeLevel());
            if (request.getSchoolName() != null) student.setSchoolName(request.getSchoolName());
        }

        User updated = userRepository.save(currentUser);
        return mapToProfileResponse(updated);
    }

    private UserProfileResponse mapToProfileResponse(User user) {
        UserProfileResponse.UserProfileResponseBuilder builder = UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .createdAt(user.getCreatedAt());

        if (user instanceof Tutor) {
            Tutor t = (Tutor) user;
            builder.bio(t.getBio())
                   .qualification(t.getQualification())
                   .experienceYears(t.getExperienceYears())
                   .hourlyRate(t.getHourlyRate());
        } else if (user instanceof Parent) {
            Parent p = (Parent) user;
            builder.address(p.getAddress())
                   .emergencyContact(p.getEmergencyContact());
        } else if (user instanceof Student) {
            Student s = (Student) user;
            builder.gradeLevel(s.getGradeLevel())
                   .schoolName(s.getSchoolName());
        }

        return builder.build();
    }
}
