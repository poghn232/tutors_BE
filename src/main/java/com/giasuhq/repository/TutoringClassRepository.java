package com.giasuhq.repository;

import com.giasuhq.entity.TutoringClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TutoringClassRepository extends JpaRepository<TutoringClass, Long> {
    List<TutoringClass> findByTutorId(Long tutorId);
    List<TutoringClass> findByParentId(Long parentId);
    List<TutoringClass> findByStudentId(Long studentId);
}
