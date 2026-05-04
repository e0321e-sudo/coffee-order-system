package com.coffee.order.domain.menu.service;

import com.coffee.order.common.exception.BusinessException;
import com.coffee.order.common.exception.ErrorCode;
import com.coffee.order.domain.menu.dto.response.MenuResponseDto;
import com.coffee.order.domain.menu.dto.response.PopularMenuResponseDto;
import com.coffee.order.domain.menu.entity.Category;
import com.coffee.order.domain.menu.entity.Menu;
import com.coffee.order.domain.menu.entity.MenuStock;
import com.coffee.order.domain.menu.repository.CategoryRepository;
import com.coffee.order.domain.menu.repository.MenuRepository;
import com.coffee.order.domain.menu.repository.MenuStockRepository;
import com.coffee.order.domain.order.entity.OrderStatus;
import com.coffee.order.domain.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuRepository menuRepository;
    private final CategoryRepository categoryRepository;
    private final MenuStockRepository menuStockRepository;
    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public List<MenuResponseDto> getMenus(Long storeId, Long categoryId) {
        List<Menu> menus = (categoryId != null)
                ? menuRepository.findAllByCategoryIdAndIsVisibleTrue(categoryId)
                : menuRepository.findAllByIsVisibleTrue();

        Set<Long> categoryIds = menus.stream().map(Menu::getCategoryId).collect(Collectors.toSet());
        Map<Long, String> categoryNames = categoryRepository.findAllById(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));

        Map<Long, Boolean> soldOutMap = buildSoldOutMap(storeId, menus);

        return menus.stream()
                .map(m -> new MenuResponseDto(
                        m.getId(), m.getName(), m.getPrice(),
                        categoryNames.get(m.getCategoryId()),
                        soldOutMap.getOrDefault(m.getId(), false)
                ))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MenuResponseDto getMenu(Long menuId, Long storeId) {
        Menu menu = menuRepository.findByIdAndIsVisibleTrue(menuId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MENU_NOT_FOUND));

        String categoryName = categoryRepository.findById(menu.getCategoryId())
                .map(Category::getName)
                .orElse(null);

        Map<Long, Boolean> soldOutMap = buildSoldOutMap(storeId, List.of(menu));

        return new MenuResponseDto(
                menu.getId(), menu.getName(), menu.getPrice(),
                categoryName, soldOutMap.getOrDefault(menuId, false)
        );
    }

    @Transactional(readOnly = true)
    public List<PopularMenuResponseDto> getPopularMenus() {
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        List<Object[]> rows = orderRepository.findTopMenuIdsByOrderCount(
                OrderStatus.COMPLETED.name(), since
        );

        if (rows.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> menuIds = rows.stream()
                .map(r -> ((Number) r[0]).longValue())
                .collect(Collectors.toList());

        Map<Long, Long> orderCountMap = rows.stream()
                .collect(Collectors.toMap(
                        r -> ((Number) r[0]).longValue(),
                        r -> ((Number) r[1]).longValue()
                ));

        Map<Long, Menu> menuMap = menuRepository.findAllById(menuIds).stream()
                .collect(Collectors.toMap(Menu::getId, m -> m));

        Set<Long> categoryIds = menuMap.values().stream()
                .map(Menu::getCategoryId).collect(Collectors.toSet());
        Map<Long, String> categoryNames = categoryRepository.findAllById(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));

        return menuIds.stream()
                .filter(menuMap::containsKey)
                .map(id -> {
                    Menu m = menuMap.get(id);
                    return new PopularMenuResponseDto(
                            m.getId(), m.getName(), m.getPrice(),
                            categoryNames.get(m.getCategoryId()),
                            orderCountMap.get(id)
                    );
                })
                .collect(Collectors.toList());
    }

    private Map<Long, Boolean> buildSoldOutMap(Long storeId, List<Menu> menus) {
        if (storeId == null || menus.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> menuIds = menus.stream().map(Menu::getId).collect(Collectors.toList());
        return menuStockRepository.findByStoreIdAndMenuIdIn(storeId, menuIds).stream()
                .collect(Collectors.toMap(MenuStock::getMenuId, MenuStock::isSoldOut));
    }
}