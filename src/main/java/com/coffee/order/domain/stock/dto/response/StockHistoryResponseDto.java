package com.coffee.order.domain.stock.dto.response;

import com.coffee.order.domain.stock.entity.StockHistory;
import com.coffee.order.domain.stock.entity.StockHistoryType;

import java.time.LocalDateTime;

public record StockHistoryResponseDto(
        Long id,
        Long menuId,
        Long storeId,
        StockHistoryType type,
        int changedAmount,
        int stockBefore,
        int stockAfter,
        Long adminId,
        LocalDateTime createdAt
) {
    public static StockHistoryResponseDto from(StockHistory h) {
        return new StockHistoryResponseDto(
                h.getId(), h.getMenuId(), h.getStoreId(), h.getType(),
                h.getChangedAmount(), h.getStockBefore(), h.getStockAfter(),
                h.getAdminId(), h.getCreatedAt());
    }
}