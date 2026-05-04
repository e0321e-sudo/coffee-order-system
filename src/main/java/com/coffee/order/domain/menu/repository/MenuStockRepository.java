package com.coffee.order.domain.menu.repository;

import com.coffee.order.domain.menu.entity.MenuStock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MenuStockRepository extends JpaRepository<MenuStock, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ms FROM MenuStock ms WHERE ms.storeId = :storeId AND ms.menuId = :menuId")
    Optional<MenuStock> findByStoreIdAndMenuIdWithLock(@Param("storeId") Long storeId, @Param("menuId") Long menuId);

    List<MenuStock> findByStoreIdAndMenuIdIn(Long storeId, List<Long> menuIds);
}