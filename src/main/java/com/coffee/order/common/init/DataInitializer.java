package com.coffee.order.common.init;

import com.coffee.order.domain.admin.entity.Admin;
import com.coffee.order.domain.admin.repository.AdminRepository;
import com.coffee.order.domain.kiosk.entity.Kiosk;
import com.coffee.order.domain.kiosk.repository.KioskRepository;
import com.coffee.order.domain.menu.entity.Menu;
import com.coffee.order.domain.menu.entity.MenuStock;
import com.coffee.order.domain.menu.repository.MenuRepository;
import com.coffee.order.domain.menu.repository.MenuStockRepository;
import com.coffee.order.domain.store.entity.Store;
import com.coffee.order.domain.store.repository.StoreRepository;
import com.coffee.order.domain.user.entity.User;
import com.coffee.order.domain.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final StoreRepository storeRepository;
    private final MenuRepository menuRepository;
    private final KioskRepository kioskRepository;
    private final MenuStockRepository menuStockRepository;
    private final UserRepository userRepository; // 1. UserRepository 추가

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        createAdminIfAbsent();
        createTestData();
    }

    private void createAdminIfAbsent() {
        if (adminRepository.findByEmail("admin@test.com").isPresent()) return;
        adminRepository.save(Admin.builder()
                .email("admin@test.com")
                .password(passwordEncoder.encode("1234"))
                .build());
    }

    private void createTestData() {
        if (storeRepository.count() == 0) {
            // 1. 매장 생성
            Store store = storeRepository.save(Store.builder()
                    .name("강남점")
                    .address("서울시 강남구")
                    .isActive(true)
                    .build());

            // 2. 키오스크 생성
            kioskRepository.save(Kiosk.builder()
                    .storeId(store.getId())
                    .kioskUuid("test-kiosk-001")
                    .secretKey("test-secret-123")
                    .name("1번 키오스크")
                    .isActive(true)
                    .build());

            // 3. 메뉴 생성
            Menu menu = menuRepository.save(Menu.builder()
                    .categoryId(1L)
                    .name("아메리카노")
                    .price(4500)
                    .isVisible(true)
                    .build());

            // 4. 메뉴 재고 생성
            menuStockRepository.save(MenuStock.builder()
                    .storeId(store.getId())
                    .menuId(menu.getId())
                    .stock(600) // 재고 넉넉히
                    .build());

            // 5. 테스트 유저 생성 및 포인트 충전 (핵심!)
            // k6 스크립트에서 사용하는 "010-1234-5678"과 일치시켰습니다.
            userRepository.save(User.builder()
                    .phoneNumber("010-1234-5678")
                    .point(1000000) // 100만 포인트 충전
                    .build());

            log.info("[DataInitializer] 모든 테스트 데이터 생성 완료! (Store: {}, Menu: {}, User: 010-1234-5678)",
                    store.getId(), menu.getId());
        }
    }
}