package com.giasuhq.repository;

import com.giasuhq.entity.LessonNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface LessonNoteRepository extends JpaRepository<LessonNote, Long> {
    Optional<LessonNote> findByLessonId(Long lessonId);

    @Query("SELECT n FROM LessonNote n WHERE n.lesson.id IN :lessonIds")
    List<LessonNote> findByLessonIdIn(@Param("lessonIds") Collection<Long> lessonIds);
}
