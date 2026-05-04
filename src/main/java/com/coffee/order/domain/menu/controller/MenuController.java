package com.coffee.order.domain.menu.controller;

import com.coffee.order.common.response.ApiResponse;
import com.coffee.order.domain.menu.dto.response.MenuResponseDto;
import com.coffee.order.domain.menu.dto.response.PopularMenuResponseDto;
import com.coffee.order.domain.menu.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @GetMapping
    public ApiResponse<List<MenuResponseDto>> getMenus(
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) Long categoryId) {
        return ApiResponse.success(menuService.getMenus(storeId, categoryId));
    }

    @GetMapping("/popular")
    public ApiResponse<List<PopularMenuResponseDto>> getPopularMenus() {
        return ApiResponse.success(menuService.getPopularMenus());
    }

    @GetMapping("/{menuId}")
    public ApiResponse<MenuResponseDto> getMenu(
            @PathVariable Long menuId,
            @RequestParam(required = false) Long storeId) {
        return ApiResponse.success(menuService.getMenu(menuId, storeId));
    }
}