package com.giasuhq.repository;

import com.giasuhq.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {

    @Query("SELECT l FROM Lesson l " +
           "JOIN FETCH l.tutoringClass tc " +
           "LEFT JOIN FETCH tc.subject " +
           "LEFT JOIN FETCH tc.tutor " +
           "LEFT JOIN FETCH tc.parent " +
           "WHERE tc.tutor.id = :tutorId " +
           "ORDER BY l.startTime DESC")
    List<Lesson> findByTutoringClass_Tutor_IdOrderByStartTimeDesc(@Param("tutorId") Long tutorId);

    @Query("SELECT l FROM Lesson l " +
           "JOIN FETCH l.tutoringClass tc " +
           "LEFT JOIN FETCH tc.subject " +
           "LEFT JOIN FETCH tc.tutor " +
           "LEFT JOIN FETCH tc.parent " +
           "WHERE tc.parent.id = :parentId " +
           "ORDER BY l.startTime DESC")
    List<Lesson> findByTutoringClass_Parent_IdOrderByStartTimeDesc(@Param("parentId") Long parentId);

    @Query("SELECT l FROM Lesson l " +
           "JOIN FETCH l.tutoringClass tc " +
           "LEFT JOIN FETCH tc.subject " +
           "LEFT JOIN FETCH tc.tutor " +
           "LEFT JOIN FETCH tc.parent " +
           "WHERE tc.id = :classId " +
           "ORDER BY l.startTime DESC")
    List<Lesson> findByTutoringClassIdOrderByStartTimeDesc(@Param("classId") Long classId);

    @Query("SELECT l FROM Lesson l " +
           "JOIN FETCH l.tutoringClass tc " +
           "LEFT JOIN FETCH tc.subject " +
           "LEFT JOIN FETCH tc.tutor " +
           "LEFT JOIN FETCH tc.parent " +
           "ORDER BY l.startTime DESC")
    List<Lesson> findAllByOrderByStartTimeDesc();
}
