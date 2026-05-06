package com.coffee.order.domain.order.entity;

import com.coffee.order.common.exception.BusinessException;
import com.coffee.order.common.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/** 주문 엔티티 — 상태 전이 및 취소 가능 여부 검증 */
@Entity
@Table(
    name = "orders",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_order_user_menu_kiosk_second",
        columnNames = {"user_id", "menu_id", "kiosk_id", "order_second"}
    )
)
@Getter
@NoArgsConstructor
public class Order {

    private static final long CANCEL_LIMIT_MINUTES = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "menu_id", nullable = false)
    private Long menuId;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "kiosk_id", nullable = false)
    private Long kioskId;

    @Column(name = "total_price", nullable = false)
    private int totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "order_second", nullable = false, updatable = false)
    private LocalDateTime orderSecond;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Builder
    public Order(Long userId, Long menuId, Long storeId, Long kioskId, int totalPrice) {
        this.userId = userId;
        this.menuId = menuId;
        this.storeId = storeId;
        this.kioskId = kioskId;
        this.totalPrice = totalPrice;
        this.status = OrderStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.orderSecond = this.createdAt.truncatedTo(ChronoUnit.SECONDS);
    }

    /** 주문 완료 처리 */
    public void complete() {
        this.status = OrderStatus.COMPLETED;
    }

    /** 주문 취소 처리 — 취소 시각 기록 */
    public void cancel() {
        this.status = OrderStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
    }

    /** 취소 가능 여부 검증 — 주문 후 5분 초과 시 ORDER_CANCEL_EXPIRED 예외 */
    public void validateCancelable() {
        if (LocalDateTime.now().isAfter(createdAt.plusMinutes(CANCEL_LIMIT_MINUTES))) {
            throw new BusinessException(ErrorCode.ORDER_CANCEL_EXPIRED);
        }
    }
}