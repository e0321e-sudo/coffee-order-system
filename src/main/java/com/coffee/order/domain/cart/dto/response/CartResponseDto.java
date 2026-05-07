package com.coffee.order.domain.cart.dto.response;

import java.util.List;

public record CartResponseDto(
        String kioskUuid,
        List<CartItemResponseDto> items,
        int totalAmount
) {
}