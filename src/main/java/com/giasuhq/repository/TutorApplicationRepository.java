package com.giasuhq.repository;

import com.giasuhq.entity.TutorApplication;
import com.giasuhq.entity.TutorApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TutorApplicationRepository extends JpaRepository<TutorApplication, Long> {

    List<TutorApplication> findByPostIdOrderByCreatedAtDesc(Long postId);

    List<TutorApplication> findByTutorIdOrderByCreatedAtDesc(Long tutorId);

    Optional<TutorApplication> findByPostIdAndTutorId(Long postId, Long tutorId);

    boolean existsByPostIdAndTutorId(Long postId, Long tutorId);

    long countByPostId(Long postId);

    long countByPostIdAndStatus(Long postId, TutorApplicationStatus status);

    List<TutorApplication> findByPostIdAndStatus(Long postId, TutorApplicationStatus status);
}
