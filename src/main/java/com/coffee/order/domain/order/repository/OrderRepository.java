package com.coffee.order.domain.order.repository;

import com.coffee.order.domain.order.entity.Order;
import com.coffee.order.domain.order.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByIdAndUserId(Long id, Long userId);

    long countByMenuIdAndStatusAndCreatedAtAfter(Long menuId, OrderStatus status, LocalDateTime createdAt);

    // 최근 N일 기준 메뉴별 주문 수 집계 (상위 3개)
    @Query(value = "SELECT menu_id, COUNT(*) AS cnt FROM orders " +
            "WHERE status = :status AND created_at > :since " +
            "GROUP BY menu_id ORDER BY cnt DESC LIMIT 3",
            nativeQuery = true)
    List<Object[]> findTopMenuIdsByOrderCount(@Param("status") String status,
                                              @Param("since") LocalDateTime since);
}