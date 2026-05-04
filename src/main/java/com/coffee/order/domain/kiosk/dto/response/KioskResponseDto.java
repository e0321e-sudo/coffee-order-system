package com.coffee.order.domain.kiosk.dto.response;

import com.coffee.order.domain.kiosk.entity.Kiosk;

public record KioskResponseDto(Long id, Long storeId, String kioskUuid, String secretKey, String name, boolean isActive) {

    public static KioskResponseDto from(Kiosk kiosk) {
        return new KioskResponseDto(
                kiosk.getId(), kiosk.getStoreId(), kiosk.getKioskUuid(),
                kiosk.getSecretKey(), kiosk.getName(), kiosk.isActive());
    }
}