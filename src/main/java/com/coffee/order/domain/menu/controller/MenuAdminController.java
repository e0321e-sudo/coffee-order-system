package com.coffee.order.domain.menu.controller;

import com.coffee.order.common.response.ApiResponse;
import com.coffee.order.domain.menu.dto.request.MenuAdminRequestDto;
import com.coffee.order.domain.menu.dto.request.StockAddRequestDto;
import com.coffee.order.domain.menu.dto.response.MenuResponseDto;
import com.coffee.order.domain.menu.service.MenuAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/menus")
@RequiredArgsConstructor
public class MenuAdminController {

    private final MenuAdminService menuAdminService;

    @PostMapping
    public ApiResponse<MenuResponseDto> create(@Valid @RequestBody MenuAdminRequestDto request) {
        return ApiResponse.success(menuAdminService.create(request));
    }

    @GetMapping
    public ApiResponse<List<MenuResponseDto>> findAll() {
        return ApiResponse.success(menuAdminService.findAll());
    }

    @PutMapping("/{id}")
    public ApiResponse<MenuResponseDto> update(@PathVariable Long id,
                                                @Valid @RequestBody MenuAdminRequestDto request) {
        return ApiResponse.success(menuAdminService.update(id, request));
    }

    @PatchMapping("/{id}/hide")
    public ApiResponse<MenuResponseDto> hide(@PathVariable Long id) {
        return ApiResponse.success(menuAdminService.hide(id));
    }

    @PostMapping("/{menuId}/stocks")
    public ApiResponse<Void> addStock(@PathVariable Long menuId,
                                       @Valid @RequestBody StockAddRequestDto request) {
        menuAdminService.addStock(menuId, request);
        return ApiResponse.success(null);
    }
}