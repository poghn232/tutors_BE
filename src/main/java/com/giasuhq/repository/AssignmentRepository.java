package com.giasuhq.repository;

import com.giasuhq.entity.Assignment;
import com.giasuhq.entity.AssignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    List<Assignment> findByTutorIdOrderByCreatedAtDesc(Long tutorId);

    List<Assignment> findByParentIdOrderByCreatedAtDesc(Long parentId);

    List<Assignment> findByTutoringClassIdOrderByCreatedAtDesc(Long classId);

    @Query("SELECT a FROM Assignment a WHERE a.status = :status AND a.dueDate < :now")
    List<Assignment> findOverduePendingAssignments(@Param("status") AssignmentStatus status, @Param("now") LocalDateTime now);
}
