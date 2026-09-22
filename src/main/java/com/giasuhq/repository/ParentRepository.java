package com.giasuhq.repository;

import com.giasuhq.entity.Parent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParentRepository extends JpaRepository<Parent, Long> {
    Optional<Parent> findByEmail(String email);
    Optional<Parent> findByEmailIgnoreCase(String email);

    @Query("SELECT p FROM Parent p WHERE " +
           "LOWER(p.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.phone) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.studentName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Parent> searchParents(@Param("keyword") String keyword);
}

