package com.giasuhq.service.impl;

import com.giasuhq.dto.request.ReviewRequest;
import com.giasuhq.dto.response.ReviewResponse;
import com.giasuhq.dto.response.TutorReviewsSummaryResponse;
import com.giasuhq.entity.Review;
import com.giasuhq.entity.Tutor;
import com.giasuhq.entity.User;
import com.giasuhq.exception.ResourceNotFoundException;
import com.giasuhq.repository.ReviewRepository;
import com.giasuhq.repository.TutorRepository;
import com.giasuhq.repository.UserRepository;
import com.giasuhq.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final TutorRepository tutorRepository;
    private final UserRepository userRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    @Transactional(readOnly = true)
    public TutorReviewsSummaryResponse getReviewsForTutor(Long tutorId) {
        if (!tutorRepository.existsById(tutorId)) {
            throw new ResourceNotFoundException("Không tìm thấy gia sư với ID: " + tutorId);
        }

        List<Review> reviews = reviewRepository.findByTutorIdOrderByCreatedAtDesc(tutorId);
        Double avgRating = reviewRepository.getAverageRatingByTutorId(tutorId);
        double roundedRating = avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0;

        List<ReviewResponse> mapped = reviews.stream()
                .map(this::mapToResponse)
                .toList();

        return new TutorReviewsSummaryResponse(
                tutorId,
                roundedRating,
                reviews.size(),
                mapped
        );
    }

    @Override
    @Transactional
    public ReviewResponse createReview(Long tutorId, ReviewRequest request, String currentUserEmail) {
        Tutor tutor = tutorRepository.findById(tutorId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy gia sư với ID: " + tutorId));

        User user = null;
        if (currentUserEmail != null && !currentUserEmail.isBlank()) {
            user = userRepository.findByEmail(currentUserEmail).orElse(null);
        }

        String authorName = request.authorName();
        if (authorName == null || authorName.isBlank()) {
            if (user != null && user.getFullName() != null && !user.getFullName().isBlank()) {
                authorName = user.getFullName();
            } else {
                authorName = "Phụ huynh";
            }
        }

        Review review = Review.builder()
                .tutor(tutor)
                .user(user)
                .authorName(authorName.trim())
                .rating(request.rating())
                .comment(request.comment().trim())
                .build();

        Review saved = reviewRepository.save(review);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Double getAverageRating(Long tutorId) {
        Double avg = reviewRepository.getAverageRatingByTutorId(tutorId);
        return avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0;
    }

    @Override
    @Transactional(readOnly = true)
    public int getReviewsCount(Long tutorId) {
        return (int) reviewRepository.countByTutorId(tutorId);
    }

    private ReviewResponse mapToResponse(Review review) {
        String formattedDate = review.getCreatedAt() != null
                ? review.getCreatedAt().format(DATE_FORMATTER)
                : "";

        return new ReviewResponse(
                review.getId(),
                review.getTutor() != null ? review.getTutor().getId() : null,
                review.getUser() != null ? review.getUser().getId() : null,
                review.getAuthorName(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt(),
                formattedDate
        );
    }
}
