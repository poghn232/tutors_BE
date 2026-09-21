package com.giasuhq.service;

import com.giasuhq.dto.request.CreateTutorRequest;
import com.giasuhq.dto.response.TutorResponse;
import java.util.List;

public interface TutorService {

    List<TutorResponse> findAll();

    List<TutorResponse> findAllForAdmin();

    TutorResponse findById(Long id);

    TutorResponse create(CreateTutorRequest request);

    TutorResponse updateVerificationStatus(Long tutorId, String status, String reason);
}
