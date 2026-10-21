package com.giasuhq.repository;

import com.giasuhq.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByTutorIdOrderByCreatedAtDesc(Long tutorId);

    long countByTutorId(Long tutorId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.tutor.id = :tutorId")
    Double getAverageRatingByTutorId(@Param("tutorId") Long tutorId);

    @Query("SELECT r.tutor.id, AVG(r.rating), COUNT(r.id) FROM Review r GROUP BY r.tutor.id")
    List<Object[]> getRatingSummariesForAllTutors();
}
