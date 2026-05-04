package com.coffee.order.domain.menu.dto.response;

public record PopularMenuResponseDto(Long menuId, String name, int price, String categoryName, long orderCount) {
}