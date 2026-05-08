package com.coffee.order.domain.menu.scheduler;

import com.coffee.order.domain.menu.service.MenuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PopularMenuScheduler {

    private final MenuService menuService;

    // 매일 자정(00:00:00)에 실행
    // cron 표현식: "초 분 시 일 월 요일"
    // "0 0 0 * * *" = 매일 0시 0분 0초
    @Scheduled(cron = "0 0 0 * * *")
    public void refreshPopularMenuCache() {
        log.info("[스케줄러] 인기 메뉴 캐시 갱신 시작");

        // 기존 캐시 삭제
        menuService.clearPopularMenuCache();

        // DB에서 새로 조회해서 캐시 저장
        menuService.getPopularMenus();

        log.info("[스케줄러] 인기 메뉴 캐시 갱신 완료");
    }
}
