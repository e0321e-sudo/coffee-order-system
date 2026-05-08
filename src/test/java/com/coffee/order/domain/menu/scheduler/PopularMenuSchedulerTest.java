package com.coffee.order.domain.menu.scheduler;

import com.coffee.order.domain.menu.service.MenuService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.mockito.InOrder;

import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class PopularMenuSchedulerTest {

    @InjectMocks
    private PopularMenuScheduler popularMenuScheduler;

    @Mock
    private MenuService menuService;

    // ===== refreshPopularMenuCache =====

    @Test
    @DisplayName("캐시 갱신 - clearPopularMenuCache 호출")
    void 캐시_갱신_clearPopularMenuCache_호출() {
        // when
        popularMenuScheduler.refreshPopularMenuCache();

        // then
        then(menuService).should(times(1)).clearPopularMenuCache();
    }

    @Test
    @DisplayName("캐시 갱신 - getPopularMenus 호출")
    void 캐시_갱신_getPopularMenus_호출() {
        // when
        popularMenuScheduler.refreshPopularMenuCache();

        // then
        then(menuService).should(times(1)).getPopularMenus();
    }

    @Test
    @DisplayName("캐시 갱신 - clearPopularMenuCache 먼저, getPopularMenus 나중에 호출")
    void 캐시_갱신_호출_순서_검증() {
        // when
        popularMenuScheduler.refreshPopularMenuCache();

        // then: 삭제 후 재조회 순서 보장
        InOrder inOrder = inOrder(menuService);
        inOrder.verify(menuService).clearPopularMenuCache();
        inOrder.verify(menuService).getPopularMenus();
    }

    @Test
    @DisplayName("캐시 갱신 - 각 메서드 정확히 1회 호출")
    void 캐시_갱신_각_메서드_1회_호출() {
        // when
        popularMenuScheduler.refreshPopularMenuCache();

        // then
        then(menuService).should(times(1)).clearPopularMenuCache();
        then(menuService).should(times(1)).getPopularMenus();
        then(menuService).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("캐시 갱신 - clearPopularMenuCache 예외 발생 시 getPopularMenus 미호출")
    void 캐시_갱신_삭제_예외_시_재조회_미호출() {
        // given
        willThrow(new RuntimeException("Redis 연결 실패")).given(menuService).clearPopularMenuCache();

        // when & then
        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> popularMenuScheduler.refreshPopularMenuCache()
        ).isInstanceOf(RuntimeException.class);

        then(menuService).should(never()).getPopularMenus();
    }
}