package com.coffee.order.domain.menu.service;

import com.coffee.order.common.exception.BusinessException;
import com.coffee.order.common.exception.ErrorCode;
import com.coffee.order.domain.menu.dto.request.MenuAdminRequestDto;
import com.coffee.order.domain.menu.dto.request.StockAddRequestDto;
import com.coffee.order.domain.menu.dto.response.MenuResponseDto;
import com.coffee.order.domain.menu.entity.Category;
import com.coffee.order.domain.menu.entity.Menu;
import com.coffee.order.domain.menu.entity.MenuStock;
import com.coffee.order.domain.menu.repository.CategoryRepository;
import com.coffee.order.domain.menu.repository.MenuRepository;
import com.coffee.order.domain.menu.repository.MenuStockRepository;
import com.coffee.order.domain.stock.entity.StockHistory;
import com.coffee.order.domain.stock.entity.StockHistoryType;
import com.coffee.order.domain.stock.repository.StockHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuAdminService {

    private final MenuRepository menuRepository;
    private final CategoryRepository categoryRepository;
    private final MenuStockRepository menuStockRepository;
    private final StockHistoryRepository stockHistoryRepository;

    @Transactional
    public MenuResponseDto create(MenuAdminRequestDto request) {
        Menu menu = Menu.builder()
                .categoryId(request.getCategoryId())
                .name(request.getName())
                .price(request.getPrice())
                .isVisible(true)
                .build();
        menuRepository.save(menu);
        String categoryName = categoryRepository.findById(request.getCategoryId())
                .map(Category::getName).orElse(null);
        return new MenuResponseDto(menu.getId(), menu.getName(), menu.getPrice(), categoryName, false);
    }

    @Transactional(readOnly = true)
    public List<MenuResponseDto> findAll() {
        List<Menu> menus = menuRepository.findAllByIsVisibleTrue();
        Set<Long> categoryIds = menus.stream().map(Menu::getCategoryId).collect(Collectors.toSet());
        Map<Long, String> categoryNames = categoryRepository.findAllById(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));
        return menus.stream()
                .map(m -> new MenuResponseDto(
                        m.getId(), m.getName(), m.getPrice(),
                        categoryNames.get(m.getCategoryId()), false))
                .toList();
    }

    @Transactional
    public MenuResponseDto update(Long id, MenuAdminRequestDto request) {
        Menu menu = getMenu(id);
        menu.update(request.getCategoryId(), request.getName(), request.getPrice());
        String categoryName = categoryRepository.findById(request.getCategoryId())
                .map(Category::getName).orElse(null);
        return new MenuResponseDto(menu.getId(), menu.getName(), menu.getPrice(), categoryName, false);
    }

    @Transactional
    public MenuResponseDto hide(Long id) {
        Menu menu = getMenu(id);
        menu.hide();
        String categoryName = categoryRepository.findById(menu.getCategoryId())
                .map(Category::getName).orElse(null);
        return new MenuResponseDto(menu.getId(), menu.getName(), menu.getPrice(), categoryName, false);
    }

    @Transactional
    public void addStock(Long menuId, StockAddRequestDto request) {
        getMenu(menuId);
        MenuStock stock = menuStockRepository
                .findByStoreIdAndMenuIdWithLock(request.getStoreId(), menuId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STOCK_NOT_FOUND));
        int before = stock.getStock();
        stock.addStock(request.getAmount());
        stockHistoryRepository.save(StockHistory.builder()
                .menuId(menuId)
                .storeId(request.getStoreId())
                .type(StockHistoryType.RESTOCK)
                .changedAmount(request.getAmount())
                .stockBefore(before)
                .stockAfter(stock.getStock())
                .adminId(null)
                .build());
    }

    private Menu getMenu(Long id) {
        return menuRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.MENU_NOT_FOUND));
    }
}