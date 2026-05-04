package com.coffee.order.domain.stock.controller;

import com.coffee.order.common.response.ApiResponse;
import com.coffee.order.domain.stock.dto.response.StockHistoryResponseDto;
import com.coffee.order.domain.stock.service.StockHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/stock-histories")
@RequiredArgsConstructor
public class StockHistoryController {

    private final StockHistoryService stockHistoryService;

    @GetMapping
    public ApiResponse<List<StockHistoryResponseDto>> find(
            @RequestParam Long menuId,
            @RequestParam(required = false) Long storeId) {
        if (storeId != null) {
            return ApiResponse.success(stockHistoryService.findByMenuAndStore(menuId, storeId));
        }
        return ApiResponse.success(stockHistoryService.findByMenu(menuId));
    }
}