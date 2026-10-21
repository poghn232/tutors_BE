package com.giasuhq.repository;

import com.giasuhq.entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Long> {
    Optional<Voucher> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);
    List<Voucher> findByActiveTrueOrderByCreatedAtAsc();
    List<Voucher> findAllByOrderByCreatedAtDesc();
}
