package com.coffee.order.domain.cart.dto.response;

public record CartItemResponseDto(
        Long menuId,
        String menuName,
        int price,
        int quantity,
        int subtotal
) {
}