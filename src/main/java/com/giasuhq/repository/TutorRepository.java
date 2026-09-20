package com.giasuhq.repository;

import com.giasuhq.entity.Tutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TutorRepository extends JpaRepository<Tutor, Long> {
    Optional<Tutor> findByEmail(String email);
    Optional<Tutor> findByEmailIgnoreCase(String email);
    Optional<Tutor> findFirstByFullNameContainingIgnoreCase(String name);
}
