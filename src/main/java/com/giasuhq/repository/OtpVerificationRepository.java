package com.giasuhq.repository;

import com.giasuhq.entity.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    Optional<OtpVerification> findTopByEmailAndPurposeOrderByCreatedAtDesc(String email, String purpose);

    void deleteByEmailAndPurpose(String email, String purpose);
}
