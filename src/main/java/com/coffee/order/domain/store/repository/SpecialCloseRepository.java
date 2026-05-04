package com.coffee.order.domain.store.repository;

import com.coffee.order.domain.store.entity.SpecialClose;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface SpecialCloseRepository extends JpaRepository<SpecialClose, Long> {

    boolean existsByStoreIdAndCloseDate(Long storeId, LocalDate closeDate);

    List<SpecialClose> findByStoreId(Long storeId);
}