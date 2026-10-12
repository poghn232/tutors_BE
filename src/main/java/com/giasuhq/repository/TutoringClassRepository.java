package com.giasuhq.repository;

import com.giasuhq.entity.TutoringClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TutoringClassRepository extends JpaRepository<TutoringClass, Long> {
    List<TutoringClass> findByTutorId(Long tutorId);
    List<TutoringClass> findByParentId(Long parentId);

    List<TutoringClass> findByTutorIdOrderByCreatedAtDesc(Long tutorId);
    List<TutoringClass> findByParentIdOrderByCreatedAtDesc(Long parentId);
    List<TutoringClass> findAllByOrderByCreatedAtDesc();

    Optional<TutoringClass> findByPaymentOrderCodeIgnoreCase(String paymentOrderCode);
}
