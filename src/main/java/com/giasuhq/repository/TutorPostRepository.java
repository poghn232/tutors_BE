package com.giasuhq.repository;

import com.giasuhq.entity.TeachingMode;
import com.giasuhq.entity.TutorPost;
import com.giasuhq.entity.TutorPostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface TutorPostRepository extends JpaRepository<TutorPost, Long> {

    List<TutorPost> findByCreatorIdOrderByCreatedAtDesc(Long creatorId);

    Page<TutorPost> findByStatusOrderByCreatedAtDesc(TutorPostStatus status, Pageable pageable);

    Page<TutorPost> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("""
        SELECT p FROM TutorPost p
        WHERE p.status = :status
          AND (:subjectId IS NULL OR p.subject.id = :subjectId)
          AND (:gradeLevel IS NULL OR :gradeLevel = '' OR LOWER(p.gradeLevel) LIKE LOWER(CONCAT('%', :gradeLevel, '%')))
          AND (:teachingMode IS NULL OR p.teachingMode = :teachingMode)
          AND (:city IS NULL OR :city = '' OR LOWER(p.city) LIKE LOWER(CONCAT('%', :city, '%')))
          AND (:district IS NULL OR :district = '' OR LOWER(p.district) LIKE LOWER(CONCAT('%', :district, '%')))
          AND (:minBudget IS NULL OR p.budgetAmount >= :minBudget)
          AND (:maxBudget IS NULL OR p.budgetAmount <= :maxBudget)
          AND (:keyword IS NULL OR :keyword = '' OR (
              LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
              OR LOWER(p.studentNotes) LIKE LOWER(CONCAT('%', :keyword, '%'))
              OR LOWER(p.requirements) LIKE LOWER(CONCAT('%', :keyword, '%'))
          ))
        ORDER BY p.createdAt DESC
    """)
    Page<TutorPost> searchPublicPosts(
            @Param("status") TutorPostStatus status,
            @Param("subjectId") Long subjectId,
            @Param("gradeLevel") String gradeLevel,
            @Param("teachingMode") TeachingMode teachingMode,
            @Param("city") String city,
            @Param("district") String district,
            @Param("minBudget") BigDecimal minBudget,
            @Param("maxBudget") BigDecimal maxBudget,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    long countByStatus(TutorPostStatus status);
}
