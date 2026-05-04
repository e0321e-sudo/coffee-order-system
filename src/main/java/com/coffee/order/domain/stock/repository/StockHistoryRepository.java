package com.coffee.order.domain.stock.repository;

import com.coffee.order.domain.stock.entity.StockHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockHistoryRepository extends JpaRepository<StockHistory, Long> {

    List<StockHistory> findByMenuIdAndStoreIdOrderByCreatedAtDesc(Long menuId, Long storeId);

    List<StockHistory> findByMenuIdOrderByCreatedAtDesc(Long menuId);
}