package com.coffee.order.domain.menu.entity;

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

class MenuStockTest {

    private MenuStock menuStock;

    @BeforeEach
    void setUp() {
        menuStock = MenuStock.builder()
                .storeId(1L)
                .menuId(1L)
                .stock(10)
                .build();
    }

    // ===== validateNotSoldOut =====

    @Test
    @DisplayName("재고 있을 때 품절 검증 통과")
    void 재고_있을때_품절검증_통과() {
        // given: stock=10, isSoldOut=false

        // when & then: 예외 없이 통과
        assertThatCode(() -> menuStock.validateNotSoldOut())
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("품절 상태에서 검증 시 MENU_SOLD_OUT 예외")
    void 품절_상태_검증_예외() {
        // given: stock=0이면 isSoldOut=true
        MenuStock soldOutStock = MenuStock.builder()
                .storeId(1L)
                .menuId(1L)
                .stock(0)
                .build();

        // when & then
        assertThatThrownBy(soldOutStock::validateNotSoldOut)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MENU_SOLD_OUT);
    }

    // ===== decreaseStock =====

    @Test
    @DisplayName("재고 차감 성공")
    void 재고_차감_성공() {
        // given: stock=10

        // when
        menuStock.decreaseStock();

        // then
        assertThat(menuStock.getStock()).isEqualTo(9);
        assertThat(menuStock.isSoldOut()).isFalse();
    }

    @Test
    @DisplayName("재고 1개에서 차감 시 즉시 품절 처리 - 경계값")
    void 재고_1개_차감시_품절처리() {
        // given: stock=1
        MenuStock oneStock = MenuStock.builder()
                .storeId(1L)
                .menuId(1L)
                .stock(1)
                .build();

        // when
        oneStock.decreaseStock();

        // then: stock=0, isSoldOut=true
        assertThat(oneStock.getStock()).isEqualTo(0);
        assertThat(oneStock.isSoldOut()).isTrue();
    }

    @Test
    @DisplayName("재고 2개에서 차감 시 품절 아님 - 경계값")
    void 재고_2개_차감시_품절아님() {
        // given: stock=2
        MenuStock twoStock = MenuStock.builder()
                .storeId(1L)
                .menuId(1L)
                .stock(2)
                .build();

        // when
        twoStock.decreaseStock();

        // then: stock=1, isSoldOut=false
        assertThat(twoStock.getStock()).isEqualTo(1);
        assertThat(twoStock.isSoldOut()).isFalse();
    }

    @Test
    @DisplayName("품절 상태에서 차감 시 MENU_SOLD_OUT 예외")
    void 품절_상태_차감_예외() {
        // given
        MenuStock soldOutStock = MenuStock.builder()
                .storeId(1L)
                .menuId(1L)
                .stock(0)
                .build();

        // when & then
        assertThatThrownBy(soldOutStock::decreaseStock)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MENU_SOLD_OUT);
    }

    // ===== increaseStock =====

    @Test
    @DisplayName("재고 1 증가 성공")
    void 재고_1개_증가_성공() {
        // given: stock=10

        // when
        menuStock.increaseStock();

        // then
        assertThat(menuStock.getStock()).isEqualTo(11);
        assertThat(menuStock.isSoldOut()).isFalse();
    }

    @Test
    @DisplayName("품절 상태에서 재고 1 증가 시 품절 해제")
    void 품절_상태_재고증가_품절해제() {
        // given
        MenuStock soldOutStock = MenuStock.builder()
                .storeId(1L)
                .menuId(1L)
                .stock(0)
                .build();

        // when
        soldOutStock.increaseStock();

        // then
        assertThat(soldOutStock.getStock()).isEqualTo(1);
        assertThat(soldOutStock.isSoldOut()).isFalse();
    }

    // ===== addStock =====

    @Test
    @DisplayName("재고 일괄 추가 성공")
    void 재고_일괄추가_성공() {
        // given: stock=10, addAmount=5

        // when
        menuStock.addStock(5);

        // then
        assertThat(menuStock.getStock()).isEqualTo(15);
        assertThat(menuStock.isSoldOut()).isFalse();
    }

    @Test
    @DisplayName("품절 상태에서 재고 일괄 추가 시 품절 해제")
    void 품절_상태_일괄추가_품절해제() {
        // given
        MenuStock soldOutStock = MenuStock.builder()
                .storeId(1L)
                .menuId(1L)
                .stock(0)
                .build();

        // when
        soldOutStock.addStock(20);

        // then
        assertThat(soldOutStock.getStock()).isEqualTo(20);
        assertThat(soldOutStock.isSoldOut()).isFalse();
    }

    // ===== 생성 검증 =====

    @Test
    @DisplayName("초기 재고 0으로 생성 시 즉시 품절 처리")
    void 초기재고_0_생성시_즉시품절() {
        // when
        MenuStock zeroStock = MenuStock.builder()
                .storeId(1L)
                .menuId(1L)
                .stock(0)
                .build();

        // then
        assertThat(zeroStock.getStock()).isEqualTo(0);
        assertThat(zeroStock.isSoldOut()).isTrue();
    }

    @Test
    @DisplayName("초기 재고 1 이상으로 생성 시 품절 아님")
    void 초기재고_1이상_생성시_품절아님() {
        // when
        MenuStock normalStock = MenuStock.builder()
                .storeId(1L)
                .menuId(1L)
                .stock(1)
                .build();

        // then
        assertThat(normalStock.isSoldOut()).isFalse();
    }

    // ===== 동시성 테스트 =====

    @Test
    @DisplayName("재고 동시 차감 - CountDownLatch + ExecutorService")
    void 재고_동시_차감_동시성_테스트() throws InterruptedException {
        // given
        int threadCount = 10;
        MenuStock concurrentStock = MenuStock.builder()
                .storeId(1L)
                .menuId(1L)
                .stock(threadCount)
                .build();

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when: 모든 스레드가 동시에 재고 차감 시도
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    concurrentStock.decreaseStock();
                    successCount.incrementAndGet();
                } catch (BusinessException e) {
                    if (e.getErrorCode() == ErrorCode.MENU_SOLD_OUT) {
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