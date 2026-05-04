package com.coffee.order.domain.store.dto.response;

import com.coffee.order.domain.store.entity.Store;

public record StoreResponseDto(Long id, String name, String address, boolean isActive) {

    public static StoreResponseDto from(Store store) {
        return new StoreResponseDto(store.getId(), store.getName(), store.getAddress(), store.isActive());
    }
}