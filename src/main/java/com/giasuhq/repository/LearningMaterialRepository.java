package com.giasuhq.repository;

import com.giasuhq.entity.LearningMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LearningMaterialRepository extends JpaRepository<LearningMaterial, Long> {

    List<LearningMaterial> findAllByOrderByCreatedAtDesc();

    @Query("SELECT m FROM LearningMaterial m WHERE " +
           "(:subject IS NULL OR :subject = 'all' OR LOWER(m.subjectName) = LOWER(:subject)) AND " +
           "(:type IS NULL OR :type = 'all' OR LOWER(m.materialType) = LOWER(:type)) AND " +
           "(:query IS NULL OR :query = '' OR " +
           "LOWER(m.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(m.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(m.authorName) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY m.createdAt DESC")
    List<LearningMaterial> searchMaterials(
            @Param("subject") String subject,
            @Param("type") String type,
            @Param("query") String query
    );
}
