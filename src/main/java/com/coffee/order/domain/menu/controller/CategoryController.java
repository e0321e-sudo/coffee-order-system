package com.coffee.order.domain.menu.controller;

import com.coffee.order.common.response.ApiResponse;
import com.coffee.order.domain.menu.dto.request.CategoryRequestDto;
import com.coffee.order.domain.menu.dto.response.CategoryResponseDto;
import com.coffee.order.domain.menu.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    public ApiResponse<CategoryResponseDto> create(@Valid @RequestBody CategoryRequestDto request) {
        return ApiResponse.success(categoryService.create(request));
    }

    @GetMapping
    public ApiResponse<List<CategoryResponseDto>> findAll() {
        return ApiResponse.success(categoryService.findAll());
    }

    @PutMapping("/{id}")
    public ApiResponse<CategoryResponseDto> update(@PathVariable Long id,
                                                    @Valid @RequestBody CategoryRequestDto request) {
        return ApiResponse.success(categoryService.update(id, request));
    }

    @PatchMapping("/{id}/hide")
    public ApiResponse<CategoryResponseDto> hide(@PathVariable Long id) {
        return ApiResponse.success(categoryService.hide(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ApiResponse.success(null);
    }
}