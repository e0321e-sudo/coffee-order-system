package com.coffee.order.domain.cart.dto.response;

import java.util.List;

public record CartCheckoutResponseDto(
        List<Long> orderIds,
        int totalAmount,
        long remainingPoint
) {
}