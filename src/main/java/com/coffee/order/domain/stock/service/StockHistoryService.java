package com.coffee.order.domain.stock.service;

import com.coffee.order.domain.stock.dto.response.StockHistoryResponseDto;
import com.coffee.order.domain.stock.repository.StockHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StockHistoryService {

    private final StockHistoryRepository stockHistoryRepository;

    @Transactional(readOnly = true)
    public List<StockHistoryResponseDto> findByMenu(Long menuId) {
        return stockHistoryRepository.findByMenuIdOrderByCreatedAtDesc(menuId).stream()
                .map(StockHistoryResponseDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StockHistoryResponseDto> findByMenuAndStore(Long menuId, Long storeId) {
        return stockHistoryRepository.findByMenuIdAndStoreIdOrderByCreatedAtDesc(menuId, storeId).stream()
                .map(StockHistoryResponseDto::from)
                .toList();
    }
}