package com.coffee.order.common.init;

import com.coffee.order.domain.admin.entity.Admin;
import com.coffee.order.domain.admin.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        createAdminIfAbsent();
    }

    private void createAdminIfAbsent() {
        if (adminRepository.findByEmail("admin@test.com").isPresent()) {
            return;
        }
        adminRepository.save(Admin.builder()
                .email("admin@test.com")
                .password(passwordEncoder.encode("1234"))
                .build());
        log.info("[DataInitializer] 관리자 계정 생성 완료: admin@test.com / 1234");
    }
}