package com.coffee.order.domain.menu.dto.response;

import com.coffee.order.domain.menu.entity.Category;

public record CategoryResponseDto(Long id, String name, int displayOrder, boolean isVisible) {

    public static CategoryResponseDto from(Category category) {
        return new CategoryResponseDto(
                category.getId(), category.getName(),
                category.getDisplayOrder(), category.isVisible());
    }
}