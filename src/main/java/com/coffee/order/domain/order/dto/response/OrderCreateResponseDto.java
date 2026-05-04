package com.coffee.order.domain.order.dto.response;

import com.coffee.order.domain.order.entity.Order;
import com.coffee.order.domain.order.entity.OrderStatus;

import java.time.LocalDateTime;

public record OrderCreateResponseDto(
        Long orderId,
        String menuName,
        int totalPrice,
        long remainingPoint,
        OrderStatus status,
        LocalDateTime createdAt
) {
    public static OrderCreateResponseDto of(Order order, String menuName, long remainingPoint) {
        return new OrderCreateResponseDto(
                order.getId(), menuName, order.getTotalPrice(),
                remainingPoint, order.getStatus(), order.getCreatedAt()
        );
    }
}