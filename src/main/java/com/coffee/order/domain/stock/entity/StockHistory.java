package com.coffee.order.domain.stock.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 재고 변동 이력 엔티티 — 주문 사용, 입고, 조정 내역 추적 */
@Entity
@Table(name = "stock_histories")
@Getter
@NoArgsConstructor
public class StockHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "menu_id", nullable = false)
    private Long menuId;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private StockHistoryType type;

    @Column(name = "changed_amount", nullable = false)
    private int changedAmount;

    @Column(name = "stock_before", nullable = false)
    private int stockBefore;

    @Column(name = "stock_after", nullable = false)
    private int stockAfter;

    @Column(name = "admin_id")
    private Long adminId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public StockHistory(Long menuId, Long storeId, StockHistoryType type,
                        int changedAmount, int stockBefore, int stockAfter, Long adminId) {
        this.menuId = menuId;
        this.storeId = storeId;
        this.type = type;
        this.changedAmount = changedAmount;
        this.stockBefore = stockBefore;
        this.stockAfter = stockAfter;
        this.adminId = adminId;
        this.createdAt = LocalDateTime.now();
    }
}