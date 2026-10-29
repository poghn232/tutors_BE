package com.giasuhq.repository;

import com.giasuhq.entity.Tutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TutorRepository extends JpaRepository<Tutor, Long> {

    @Query("SELECT DISTINCT t FROM Tutor t " +
           "LEFT JOIN FETCH t.subjects " +
           "WHERE LOWER(t.verificationStatus) = LOWER(:verificationStatus) " +
           "ORDER BY t.createdAt DESC")
    List<Tutor> findByVerificationStatusIgnoreCaseOrderByCreatedAtDesc(@Param("verificationStatus") String verificationStatus);

    @Query("SELECT DISTINCT t FROM Tutor t " +
           "LEFT JOIN FETCH t.subjects " +
           "ORDER BY t.createdAt DESC")
    List<Tutor> findAllWithSubjects();

    @Query("SELECT t FROM Tutor t " +
           "LEFT JOIN FETCH t.subjects " +
           "WHERE t.id = :id")
    Optional<Tutor> findByIdWithSubjects(@Param("id") Long id);

    Optional<Tutor> findByEmail(String email);
    Optional<Tutor> findByEmailIgnoreCase(String email);
    Optional<Tutor> findFirstByFullNameContainingIgnoreCase(String name);
}