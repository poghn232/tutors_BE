package com.giasuhq.repository;

import com.giasuhq.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {
    List<Lesson> findByTutoringClass_Tutor_IdOrderByStartTimeDesc(Long tutorId);
    List<Lesson> findByTutoringClass_Parent_IdOrderByStartTimeDesc(Long parentId);
    List<Lesson> findByTutoringClass_Student_IdOrderByStartTimeDesc(Long studentId);
    List<Lesson> findByTutoringClassIdOrderByStartTimeDesc(Long classId);
}
