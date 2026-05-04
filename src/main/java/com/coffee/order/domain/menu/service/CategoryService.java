package com.coffee.order.domain.menu.service;

import com.coffee.order.common.exception.BusinessException;
import com.coffee.order.common.exception.ErrorCode;
import com.coffee.order.domain.menu.dto.request.CategoryRequestDto;
import com.coffee.order.domain.menu.dto.response.CategoryResponseDto;
import com.coffee.order.domain.menu.entity.Category;
import com.coffee.order.domain.menu.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional
    public CategoryResponseDto create(CategoryRequestDto request) {
        Category category = Category.builder()
                .name(request.getName())
                .displayOrder(request.getDisplayOrder())
                .isVisible(true)
                .build();
        return CategoryResponseDto.from(categoryRepository.save(category));
    }

    @Transactional(readOnly = true)
    public List<CategoryResponseDto> findAll() {
        return categoryRepository.findAll().stream()
                .map(CategoryResponseDto::from)
                .toList();
    }

    @Transactional
    public CategoryResponseDto update(Long id, CategoryRequestDto request) {
        Category category = getCategory(id);
        category.update(request.getName(), request.getDisplayOrder());
        return CategoryResponseDto.from(category);
    }

    @Transactional
    public CategoryResponseDto hide(Long id) {
        Category category = getCategory(id);
        category.hide();
        return CategoryResponseDto.from(category);
    }

    @Transactional
    public void delete(Long id) {
        getCategory(id);
        categoryRepository.deleteById(id);
    }

    public Category getCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
    }
}