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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuRepository menuRepository;
    private final CategoryRepository categoryRepository;
    private final MenuStockRepository menuStockRepository;
    private final OrderRepository orderRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    // Redis 키 이름
    private static final String POPULAR_MENU_KEY = "popular:menus";

    // 캐시 유효시간: 24시간
    private static final long CACHE_TTL_HOURS = 24;

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
        // 1. Redis 캐시 확인
        // "popular:menus" 키로 저장된 데이터 꺼내기
        Object cached = redisTemplate.opsForValue().get(POPULAR_MENU_KEY);

        if (cached != null) {
            // 캐시 있으면 DB 조회 없이 바로 반환 (빠름!)
            log.info("[인기메뉴] 캐시 히트 - Redis에서 반환");
            return (List<PopularMenuResponseDto>) cached;
        }

        // 2. 캐시 없으면 DB에서 조회
        log.info("[인기메뉴] 캐시 미스 - DB에서 조회");
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

        List<PopularMenuResponseDto> popularMenus = menuIds.stream()
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

        // 3. 조회한 데이터를 Redis에 저장 (다음 요청부터 캐시 사용)
        // 24시간 후 자동 만료
        redisTemplate.opsForValue().set(POPULAR_MENU_KEY, popularMenus, CACHE_TTL_HOURS, TimeUnit.HOURS);
        log.info("[인기메뉴] 캐시 저장 완료 - TTL: {}시간", CACHE_TTL_HOURS);

        return popularMenus;
    }

    // 인기 메뉴 캐시 삭제 (스케줄러, 재입고 시 호출)
    public void clearPopularMenuCache() {
        redisTemplate.delete(POPULAR_MENU_KEY);
        log.info("[인기메뉴] 캐시 삭제 완료");
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