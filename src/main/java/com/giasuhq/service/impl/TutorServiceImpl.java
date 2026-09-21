package com.giasuhq.service.impl;

import com.giasuhq.dto.request.CreateTutorRequest;
import com.giasuhq.dto.response.TutorResponse;
import com.giasuhq.entity.Tutor;
import com.giasuhq.exception.ResourceNotFoundException;
import com.giasuhq.mapper.TutorMapper;
import com.giasuhq.repository.TutorRepository;
import com.giasuhq.service.TutorService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TutorServiceImpl implements TutorService {

    private final TutorRepository tutorRepository;
    private final TutorMapper tutorMapper;

    public TutorServiceImpl(TutorRepository tutorRepository, TutorMapper tutorMapper) {
        this.tutorRepository = tutorRepository;
        this.tutorMapper = tutorMapper;
    }

    @Override
    public List<TutorResponse> findAll() {
        return tutorRepository.findAll()
                .stream()
                .map(tutorMapper::toResponse)
                .toList();
    }

    @Override
    public List<TutorResponse> findAllForAdmin() {
        return tutorRepository.findAll()
                .stream()
                .sorted((a, b) -> {
                    boolean aPending = "PENDING".equalsIgnoreCase(a.getVerificationStatus());
                    boolean bPending = "PENDING".equalsIgnoreCase(b.getVerificationStatus());
                    if (aPending && !bPending) return -1;
                    if (!aPending && bPending) return 1;
                    return Long.compare(b.getId() != null ? b.getId() : 0, a.getId() != null ? a.getId() : 0);
                })
                .map(tutorMapper::toResponse)
                .toList();
    }

    @Override
    public TutorResponse findById(Long id) {
        Tutor tutor = tutorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tutor not found with id: " + id));
        return tutorMapper.toResponse(tutor);
    }

    @Override
    public TutorResponse create(CreateTutorRequest request) {
        Tutor tutor = tutorMapper.toEntity(null, request);
        return tutorMapper.toResponse(tutorRepository.save(tutor));
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public TutorResponse updateVerificationStatus(Long tutorId, String status, String reason) {
        Tutor tutor = tutorRepository.findById(tutorId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy gia sư với ID: " + tutorId));

        String upperStatus = status != null ? status.trim().toUpperCase() : "PENDING";
        tutor.setVerificationStatus(upperStatus);
        if ("APPROVED".equals(upperStatus)) {
            tutor.setVerifiedAt(java.time.LocalDateTime.now());
            tutor.setRejectionReason(null);
        } else if ("REJECTED".equals(upperStatus)) {
            tutor.setRejectionReason(reason);
        } else {
            tutor.setRejectionReason(null);
            tutor.setVerifiedAt(null);
        }

        Tutor saved = tutorRepository.save(tutor);
        return tutorMapper.toResponse(saved);
    }
}
