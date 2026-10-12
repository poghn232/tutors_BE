package com.giasuhq.repository;

import com.giasuhq.entity.UserVoucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserVoucherRepository extends JpaRepository<UserVoucher, Long> {
    List<UserVoucher> findByUserIdOrderByClaimedAtDesc(Long userId);
    Optional<UserVoucher> findByUserIdAndVoucherId(Long userId, Long voucherId);
    Optional<UserVoucher> findByShareCodeIgnoreCase(String shareCode);
}
