package com.coffee.order.domain.store.controller;

import com.coffee.order.common.response.ApiResponse;
import com.coffee.order.domain.store.dto.request.StoreRequestDto;
import com.coffee.order.domain.store.dto.response.StoreResponseDto;
import com.coffee.order.domain.store.service.StoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/stores")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;

    @PostMapping
    public ApiResponse<StoreResponseDto> create(@Valid @RequestBody StoreRequestDto request) {
        return ApiResponse.success(storeService.create(request));
    }

    @GetMapping
    public ApiResponse<List<StoreResponseDto>> findAll() {
        return ApiResponse.success(storeService.findAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<StoreResponseDto> findById(@PathVariable Long id) {
        return ApiResponse.success(storeService.findById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<StoreResponseDto> update(@PathVariable Long id,
                                                 @Valid @RequestBody StoreRequestDto request) {
        return ApiResponse.success(storeService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        storeService.delete(id);
        return ApiResponse.success(null);
    }
}