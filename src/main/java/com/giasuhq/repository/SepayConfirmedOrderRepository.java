package com.giasuhq.repository;

import com.giasuhq.entity.SepayConfirmedOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SepayConfirmedOrderRepository extends JpaRepository<SepayConfirmedOrder, String> {
}
