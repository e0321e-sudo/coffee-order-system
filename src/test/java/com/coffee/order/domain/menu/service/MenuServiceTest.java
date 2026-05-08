package com.coffee.order.domain.menu.service;

import com.coffee.order.domain.menu.dto.response.PopularMenuResponseDto;
import com.coffee.order.domain.menu.entity.Category;
import com.coffee.order.domain.menu.entity.Menu;
import com.coffee.order.domain.menu.repository.CategoryRepository;
import com.coffee.order.domain.menu.repository.MenuRepository;
import com.coffee.order.domain.menu.repository.MenuStockRepository;
import com.coffee.order.domain.order.entity.OrderStatus;
import com.coffee.order.domain.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
class MenuServiceTest {

    @InjectMocks
    private MenuService menuService;

    @Mock private MenuRepository menuRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private MenuStockRepository menuStockRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOps;

    private Menu menu1;
    private Menu menu2;
    private Category category;
    private List<PopularMenuResponseDto> cachedPopularMenus;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);

        menu1 = Menu.builder().categoryId(1L).name("아메리카노").price(4500).isVisible(true).build();
        ReflectionTestUtils.setField(menu1, "id", 1L);

        menu2 = Menu.builder().categoryId(1L).name("카페라떼").price(5000).isVisible(true).build();
        ReflectionTestUtils.setField(menu2, "id", 2L);

        category = Category.builder().name("커피").displayOrder(1).isVisible(true).build();
        ReflectionTestUtils.setField(category, "id", 1L);

        cachedPopularMenus = List.of(
                new PopularMenuResponseDto(1L, "아메리카노", 4500, "커피", 100L),
                new PopularMenuResponseDto(2L, "카페라떼", 5000, "커피", 80L)
        );
    }

    // ===== getPopularMenus - 캐시 히트 =====

    @Test
    @DisplayName("인기 메뉴 조회 - 캐시 히트 시 Redis 데이터 반환")
    void 인기메뉴_캐시_히트_Redis_반환() {
        // given: Redis에 캐시 존재
        given(valueOps.get("popular:menus")).willReturn(cachedPopularMenus);

        // when
        List<PopularMenuResponseDto> result = menuService.getPopularMenus();

        // then: 캐시 데이터 그대로 반환
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("아메리카노");
        assertThat(result.get(1).getName()).isEqualTo("카페라떼");
    }

    @Test
    @DisplayName("인기 메뉴 조회 - 캐시 히트 시 DB 조회 없음")
    void 인기메뉴_캐시_히트_DB_조회_없음() {
        // given
        given(valueOps.get("popular:menus")).willReturn(cachedPopularMenus);

        // when
        menuService.getPopularMenus();

        // then: OrderRepository, MenuRepository 호출 없음
        then(orderRepository).should(never()).findTopMenuIdsByOrderCount(any(), any());
        then(menuRepository).should(never()).findAllById(any());
    }

    // ===== getPopularMenus - 캐시 미스 =====

    @Test
    @DisplayName("인기 메뉴 조회 - 캐시 미스 시 DB 조회 후 반환")
    void 인기메뉴_캐시_미스_DB_조회() {
        // given
        given(valueOps.get("popular:menus")).willReturn(null);
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{1L, 100L});
        rows.add(new Object[]{2L, 80L});
        given(orderRepository.findTopMenuIdsByOrderCount(eq(OrderStatus.COMPLETED.name()), any()))
                .willReturn(rows);
        given(menuRepository.findAllById(any())).willReturn(List.of(menu1, menu2));
        given(categoryRepository.findAllById(any())).willReturn(List.of(category));

        // when
        List<PopularMenuResponseDto> result = menuService.getPopularMenus();

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getMenuId()).isEqualTo(1L);
        assertThat(result.get(0).getOrderCount()).isEqualTo(100L);
    }

    @Test
    @DisplayName("인기 메뉴 조회 - 캐시 미스 시 Redis에 결과 저장")
    void 인기메뉴_캐시_미스_Redis_저장() {
        // given
        given(valueOps.get("popular:menus")).willReturn(null);
        List<Object[]> rows = Collections.singletonList(new Object[]{1L, 100L});
        given(orderRepository.findTopMenuIdsByOrderCount(eq(OrderStatus.COMPLETED.name()), any()))
                .willReturn(rows);
        given(menuRepository.findAllById(any())).willReturn(List.of(menu1));
        given(categoryRepository.findAllById(any())).willReturn(List.of(category));

        // when
        menuService.getPopularMenus();

        // then: Redis에 24시간 TTL로 저장
        then(valueOps).should(times(1)).set(eq("popular:menus"), any(), eq(24L), eq(TimeUnit.HOURS));
    }

    @Test
    @DisplayName("인기 메뉴 조회 - DB 결과 없으면 빈 리스트 반환")
    void 인기메뉴_DB_결과_없음_빈_리스트() {
        // given
        given(valueOps.get("popular:menus")).willReturn(null);
        given(orderRepository.findTopMenuIdsByOrderCount(any(), any())).willReturn(Collections.emptyList());

        // when
        List<PopularMenuResponseDto> result = menuService.getPopularMenus();

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("인기 메뉴 조회 - DB 결과 없으면 캐시 저장 안 함")
    void 인기메뉴_DB_결과_없음_캐시_저장_안함() {
        // given
        given(valueOps.get("popular:menus")).willReturn(null);
        given(orderRepository.findTopMenuIdsByOrderCount(any(), any())).willReturn(Collections.emptyList());

        // when
        menuService.getPopularMenus();

        // then: Redis set 호출 없음
        then(valueOps).should(never()).set(anyString(), any(), anyLong(), any());
    }

    @Test
    @DisplayName("인기 메뉴 조회 - 주문 순서대로 정렬 유지")
    void 인기메뉴_주문_순서_정렬() {
        // given: rows는 이미 orderCount 내림차순 정렬 가정
        given(valueOps.get("popular:menus")).willReturn(null);
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{2L, 80L}); // 2번 메뉴가 1번보다 먼저 (더 많이 팔림)
        rows.add(new Object[]{1L, 50L});
        given(orderRepository.findTopMenuIdsByOrderCount(eq(OrderStatus.COMPLETED.name()), any()))
                .willReturn(rows);
        given(menuRepository.findAllById(any())).willReturn(List.of(menu1, menu2));
        given(categoryRepository.findAllById(any())).willReturn(List.of(category));

        // when
        List<PopularMenuResponseDto> result = menuService.getPopularMenus();

        // then: DB에서 온 순서(2번 → 1번) 유지
        assertThat(result.get(0).getMenuId()).isEqualTo(2L);
        assertThat(result.get(1).getMenuId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("인기 메뉴 조회 - 카테고리 이름 포함 검증")
    void 인기메뉴_카테고리_이름_포함() {
        // given
        given(valueOps.get("popular:menus")).willReturn(null);
        List<Object[]> rows = Collections.singletonList(new Object[]{1L, 100L});
        given(orderRepository.findTopMenuIdsByOrderCount(eq(OrderStatus.COMPLETED.name()), any()))
                .willReturn(rows);
        given(menuRepository.findAllById(any())).willReturn(List.of(menu1));
        given(categoryRepository.findAllById(any())).willReturn(List.of(category));

        // when
        List<PopularMenuResponseDto> result = menuService.getPopularMenus();

        // then
        assertThat(result.get(0).getCategoryName()).isEqualTo("커피");
    }

    // ===== clearPopularMenuCache =====

    @Test
    @DisplayName("인기 메뉴 캐시 삭제 - Redis delete 호출")
    void 인기메뉴_캐시_삭제_Redis_delete_호출() {
        // when
        menuService.clearPopularMenuCache();

        // then
        then(redisTemplate).should(times(1)).delete("popular:menus");
    }

    @Test
    @DisplayName("인기 메뉴 캐시 삭제 - 삭제 후 조회 시 DB에서 재조회")
    void 인기메뉴_캐시_삭제후_재조회() {
        // given: 삭제 후 캐시 없음 → DB 조회
        given(valueOps.get("popular:menus")).willReturn(null);
        List<Object[]> rows = Collections.singletonList(new Object[]{1L, 100L});
        given(orderRepository.findTopMenuIdsByOrderCount(eq(OrderStatus.COMPLETED.name()), any()))
                .willReturn(rows);
        given(menuRepository.findAllById(any())).willReturn(List.of(menu1));
        given(categoryRepository.findAllById(any())).willReturn(List.of(category));

        // when
        menuService.clearPopularMenuCache();
        menuService.getPopularMenus();

        // then: 캐시 삭제 1회 + DB 조회 1회
        then(redisTemplate).should(times(1)).delete("popular:menus");
        then(orderRepository).should(times(1)).findTopMenuIdsByOrderCount(any(), any());
    }
}