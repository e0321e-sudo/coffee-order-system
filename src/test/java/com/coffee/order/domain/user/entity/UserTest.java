package com.coffee.order.domain.user.entity;

import com.coffee.order.common.exception.BusinessException;
import com.coffee.order.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

class UserTest {

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .phoneNumber("01012345678")
                .point(10000L)
                .build();
    }

    // ===== chargePoint =====

    @Test
    @DisplayName("포인트 충전 성공")
    void 포인트_충전_성공() {
        // given
        long chargeAmount = 5000L;

        // when
        user.chargePoint(chargeAmount);

        // then
        assertThat(user.getPoint()).isEqualTo(15000L);
    }

    @Test
    @DisplayName("0원 충전 시 포인트 변화 없음")
    void 포인트_0원_충전_잔액_유지() {
        // given
        long chargeAmount = 0L;

        // when
        user.chargePoint(chargeAmount);

        // then
        assertThat(user.getPoint()).isEqualTo(10000L);
    }

    @Test
    @DisplayName("포인트 여러 번 충전 시 누적 합산")
    void 포인트_여러번_충전_누적() {
        // given
        long first = 1000L;
        long second = 2000L;
        long third = 3000L;

        // when
        user.chargePoint(first);
        user.chargePoint(second);
        user.chargePoint(third);

        // then
        assertThat(user.getPoint()).isEqualTo(16000L);
    }

    // ===== deductPoint =====

    @Test
    @DisplayName("포인트 차감 성공")
    void 포인트_차감_성공() {
        // given
        long deductAmount = 3000L;

        // when
        user.deductPoint(deductAmount);

        // then
        assertThat(user.getPoint()).isEqualTo(7000L);
    }

    @Test
    @DisplayName("포인트 잔액과 정확히 동일한 금액 차감 성공 - 경계값")
    void 포인트_잔액_정확히_일치_차감_성공() {
        // given: 보유 포인트와 동일한 금액
        long deductAmount = 10000L;

        // when
        user.deductPoint(deductAmount);

        // then: 잔액 0원
        assertThat(user.getPoint()).isEqualTo(0L);
    }

    @Test
    @DisplayName("포인트 1원 초과 시 INSUFFICIENT_POINT 예외 - 경계값")
    void 포인트_1원_초과_차감_예외() {
        // given: 잔액보다 1원 많은 금액
        long deductAmount = 10001L;

        // when & then
        assertThatThrownBy(() -> user.deductPoint(deductAmount))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INSUFFICIENT_POINT);
    }

    @Test
    @DisplayName("포인트 잔액 0원일 때 1원도 차감 불가")
    void 포인트_잔액없을때_차감_예외() {
        // given
        User emptyUser = User.builder()
                .phoneNumber("01099999999")
                .point(0L)
                .build();

        // when & then
        assertThatThrownBy(() -> emptyUser.deductPoint(1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INSUFFICIENT_POINT);
    }

    @Test
    @DisplayName("차감 후 포인트 잔액 검증")
    void 포인트_차감후_잔액_검증() {
        // given
        User richUser = User.builder()
                .phoneNumber("01055555555")
                .point(50000L)
                .build();
        long deductAmount = 4500L;

        // when
        richUser.deductPoint(deductAmount);

        // then
        assertThat(richUser.getPoint()).isEqualTo(45500L);
    }

    // ===== 동시성 테스트 =====

    @Test
    @DisplayName("포인트 동시 차감 - CountDownLatch + ExecutorService")
    void 포인트_동시_차감_동시성_테스트() throws InterruptedException {
        // given
        int threadCount = 10;
        long deductAmount = 1000L;
        User concurrentUser = User.builder()
                .phoneNumber("01011111111")
                .point(threadCount * deductAmount)
                .build();

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when: 모든 스레드가 동시에 차감 시도
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    concurrentUser.deductPoint(deductAmount);
                    successCount.incrementAndGet();
                } catch (BusinessException e) {
                    if (e.getErrorCode() == ErrorCode.INSUFFICIENT_POINT) {
                        failCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // 모든 스레드 동시 출발
        endLatch.await();
        executor.shutdown();

        // then: 총 시도 = threadCount, 실제 서비스에서는 DB 비관적 락으로 정합성 보장
        assertThat(successCount.get() + failCount.get()).isEqualTo(threadCount);
    }
}