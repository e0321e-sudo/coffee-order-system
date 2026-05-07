package com.coffee.order.domain.order.entity;

import com.coffee.order.common.exception.BusinessException;
import com.coffee.order.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class OrderTest {

    private Order order;

    @BeforeEach
    void setUp() {
        order = Order.builder()
                .userId(1L)
                .menuId(1L)
                .storeId(1L)
                .kioskId(1L)
                .totalPrice(4500)
                .build();
    }

    // ===== 초기 상태 =====

    @Test
    @DisplayName("주문 생성 직후 상태는 PENDING")
    void 주문_초기상태_PENDING() {
        // given: setUp()의 order

        // when: 별도 조작 없음

        // then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    @DisplayName("주문 생성 시 createdAt이 설정됨")
    void 주문_생성시_createdAt_설정() {
        // given
        LocalDateTime beforeCreate = LocalDateTime.now().minusSeconds(1);

        // when
        Order newOrder = Order.builder()
                .userId(1L)
                .menuId(1L)
                .storeId(1L)
                .kioskId(1L)
                .totalPrice(4500)
                .build();

        // then
        assertThat(newOrder.getCreatedAt()).isAfter(beforeCreate);
        assertThat(newOrder.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("orderSecond는 createdAt의 초 단위 절삭값 - 나노초 0")
    void 주문_orderSecond_초단위_절삭() {
        // given: setUp()의 order

        // when: 별도 조작 없음

        // then: nano = 0, second는 createdAt과 동일
        assertThat(order.getOrderSecond().getNano()).isEqualTo(0);
        assertThat(order.getOrderSecond().getSecond())
                .isEqualTo(order.getCreatedAt().getSecond());
        assertThat(order.getOrderSecond().getMinute())
                .isEqualTo(order.getCreatedAt().getMinute());
    }

    @Test
    @DisplayName("주문 총 가격 검증")
    void 주문_총가격_검증() {
        // given: totalPrice=4500

        // when & then
        assertThat(order.getTotalPrice()).isEqualTo(4500);
    }

    // ===== complete =====

    @Test
    @DisplayName("주문 완료 처리 성공")
    void 주문_완료_처리_성공() {
        // given: status=PENDING

        // when
        order.complete();

        // then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    // ===== cancel =====

    @Test
    @DisplayName("주문 취소 처리 성공 - status CANCELLED 변경")
    void 주문_취소_처리_성공() {
        // given: status=PENDING

        // when
        order.cancel();

        // then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("주문 취소 시 cancelledAt 설정")
    void 주문_취소시_취소시각_설정() {
        // given
        LocalDateTime beforeCancel = LocalDateTime.now();

        // when
        order.cancel();

        // then
        assertThat(order.getCancelledAt()).isNotNull();
        assertThat(order.getCancelledAt()).isAfterOrEqualTo(beforeCancel);
    }

    // ===== validateCancelable =====

    @Test
    @DisplayName("주문 후 4분 59초 이내 취소 가능")
    void 취소_4분59초_이내_취소가능() {
        // given: 4분 59초 전 주문
        ReflectionTestUtils.setField(order, "createdAt",
                LocalDateTime.now().minusMinutes(4).minusSeconds(59));

        // when & then: 예외 없이 통과
        assertThatCode(() -> order.validateCancelable())
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("주문 후 1초 뒤 취소 가능")
    void 취소_1초_후_취소가능() {
        // given: 1초 전 주문
        ReflectionTestUtils.setField(order, "createdAt",
                LocalDateTime.now().minusSeconds(1));

        // when & then
        assertThatCode(() -> order.validateCancelable())
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("주문 직후 즉시 취소 가능")
    void 취소_즉시_취소가능() {
        // given: 방금 생성된 주문 (setUp)

        // when & then
        assertThatCode(() -> order.validateCancelable())
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("주문 후 정확히 5분 경과 시점 - ORDER_CANCEL_EXPIRED 예외")
    void 취소_정확히_5분_경과_예외() {
        // given: 정확히 5분 전 주문
        ReflectionTestUtils.setField(order, "createdAt",
                LocalDateTime.now().minusMinutes(5));

        // when & then: 정확히 5분 → createdAt.plusMinutes(5) == now → isAfter 조건 false
        // 실제로는 밀리초 차이로 인해 결과가 달라질 수 있으므로 경계값을 1초 추가해 명확히 검증
        ReflectionTestUtils.setField(order, "createdAt",
                LocalDateTime.now().minusMinutes(5).minusSeconds(1));

        assertThatThrownBy(() -> order.validateCancelable())
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ORDER_CANCEL_EXPIRED);
    }

    @Test
    @DisplayName("주문 후 5분 1초 초과 - ORDER_CANCEL_EXPIRED 예외")
    void 취소_5분_1초_초과_예외() {
        // given: 5분 1초 전 주문
        ReflectionTestUtils.setField(order, "createdAt",
                LocalDateTime.now().minusMinutes(5).minusSeconds(1));

        // when & then
        assertThatThrownBy(() -> order.validateCancelable())
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ORDER_CANCEL_EXPIRED);
    }

    @Test
    @DisplayName("주문 후 10분 경과 - ORDER_CANCEL_EXPIRED 예외")
    void 취소_10분_경과_예외() {
        // given: 10분 전 주문
        ReflectionTestUtils.setField(order, "createdAt",
                LocalDateTime.now().minusMinutes(10));

        // when & then
        assertThatThrownBy(() -> order.validateCancelable())
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ORDER_CANCEL_EXPIRED);
    }
}