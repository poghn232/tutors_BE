package com.giasuhq.service.impl;

import com.giasuhq.dto.request.CreateTutorRequest;
import com.giasuhq.dto.response.TutorResponse;
import com.giasuhq.entity.Tutor;
import com.giasuhq.exception.ResourceNotFoundException;
import com.giasuhq.mapper.TutorMapper;
import com.giasuhq.repository.ReviewRepository;
import com.giasuhq.repository.TutorRepository;
import com.giasuhq.service.TutorService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class TutorServiceImpl implements TutorService {

    private final TutorRepository tutorRepository;
    private final TutorMapper tutorMapper;
    private final ReviewRepository reviewRepository;

    public TutorServiceImpl(TutorRepository tutorRepository, TutorMapper tutorMapper, ReviewRepository reviewRepository) {
        this.tutorRepository = tutorRepository;
        this.tutorMapper = tutorMapper;
        this.reviewRepository = reviewRepository;
    }

    private record RatingSummary(Double avg, Integer count) {}

    private Map<Long, RatingSummary> getRatingSummariesMap() {
        Map<Long, RatingSummary> map = new HashMap<>();
        List<Object[]> summaries = reviewRepository.getRatingSummariesForAllTutors();
        if (summaries != null) {
            for (Object[] row : summaries) {
                if (row != null && row.length >= 3 && row[0] != null) {
                    Long tutorId = ((Number) row[0]).longValue();
                    Double avg = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
                    Integer count = row[2] != null ? ((Number) row[2]).intValue() : 0;
                    map.put(tutorId, new RatingSummary(avg, count));
                }
            }
        }
        return map;
    }

    @Override
    public List<TutorResponse> findAll() {
        Map<Long, RatingSummary> summaries = getRatingSummariesMap();
        // Public catalog must only expose profiles approved by Admin.
        return tutorRepository.findByVerificationStatusIgnoreCaseOrderByCreatedAtDesc("APPROVED")
                .stream()
                .map(tutor -> {
                    RatingSummary summary = summaries.get(tutor.getId());
                    Double rating = summary != null ? summary.avg() : 0.0;
                    Integer count = summary != null ? summary.count() : 0;
                    return tutorMapper.toResponse(tutor, rating, count);
                })
                .toList();
    }

    @Override
    public List<TutorResponse> findAllForAdmin() {
        Map<Long, RatingSummary> summaries = getRatingSummariesMap();
        return tutorRepository.findAll()
                .stream()
                .sorted((a, b) -> {
                    boolean aPending = "PENDING".equalsIgnoreCase(a.getVerificationStatus());
                    boolean bPending = "PENDING".equalsIgnoreCase(b.getVerificationStatus());
                    if (aPending && !bPending) return -1;
                    if (!aPending && bPending) return 1;
                    return Long.compare(b.getId() != null ? b.getId() : 0, a.getId() != null ? a.getId() : 0);
                })
                .map(tutor -> {
                    RatingSummary summary = summaries.get(tutor.getId());
                    Double rating = summary != null ? summary.avg() : 0.0;
                    Integer count = summary != null ? summary.count() : 0;
                    return tutorMapper.toResponse(tutor, rating, count);
                })
                .toList();
    }

    @Override
    public TutorResponse findById(Long id) {
        Tutor tutor = tutorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tutor not found with id: " + id));
        Double avg = reviewRepository.getAverageRatingByTutorId(id);
        long count = reviewRepository.countByTutorId(id);
        return tutorMapper.toResponse(tutor, avg != null ? avg : 0.0, (int) count);
    }

    @Override
    public TutorResponse create(CreateTutorRequest request) {
        Tutor tutor = tutorMapper.toEntity(null, request);
        return tutorMapper.toResponse(tutorRepository.save(tutor), 0.0, 0);
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
        Double avg = reviewRepository.getAverageRatingByTutorId(tutorId);
        long count = reviewRepository.countByTutorId(tutorId);
        return tutorMapper.toResponse(saved, avg != null ? avg : 0.0, (int) count);
    }
}
