package com.giasuhq.mapper;

import com.giasuhq.dto.request.CreateTutorRequest;
import com.giasuhq.dto.response.TutorResponse;
import com.giasuhq.entity.Tutor;
import org.springframework.stereotype.Component;

@Component
public class TutorMapper {

    public Tutor toEntity(Long id, CreateTutorRequest request) {
        return new Tutor(id, request.fullName(), request.subject(), request.email(), request.hourlyRate());
    }

    public TutorResponse toResponse(Tutor tutor) {
        return new TutorResponse(
                tutor.getId(),
                tutor.getFullName(),
                tutor.getSubject(),
                tutor.getEmail(),
                tutor.getHourlyRate()
        );
    }
}
