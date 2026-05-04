package com.coffee.order.domain.menu.dto.response;

public record MenuResponseDto(Long menuId, String name, int price, String categoryName, boolean isSoldOut) {
}