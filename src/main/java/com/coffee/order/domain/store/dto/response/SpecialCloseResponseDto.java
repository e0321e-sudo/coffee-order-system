package com.coffee.order.domain.store.dto.response;

import com.coffee.order.domain.store.entity.SpecialClose;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record SpecialCloseResponseDto(Long id, Long storeId, LocalDate closeDate, String reason, LocalDateTime createdAt) {

    public static SpecialCloseResponseDto from(SpecialClose sc) {
        return new SpecialCloseResponseDto(
                sc.getId(), sc.getStoreId(), sc.getCloseDate(),
                sc.getReason(), sc.getCreatedAt());
    }
}