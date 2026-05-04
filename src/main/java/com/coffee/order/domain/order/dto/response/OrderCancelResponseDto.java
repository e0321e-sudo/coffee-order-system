package com.coffee.order.domain.order.dto.response;

import com.coffee.order.domain.order.entity.Order;
import com.coffee.order.domain.order.entity.OrderStatus;

import java.time.LocalDateTime;

public record OrderCancelResponseDto(
        Long orderId,
        OrderStatus status,
        long refundedPoint,
        long remainingPoint,
        LocalDateTime cancelledAt
) {
    public static OrderCancelResponseDto of(Order order, long refundedPoint, long remainingPoint) {
        return new OrderCancelResponseDto(
                order.getId(), order.getStatus(),
                refundedPoint, remainingPoint, order.getCancelledAt()
        );
    }
}