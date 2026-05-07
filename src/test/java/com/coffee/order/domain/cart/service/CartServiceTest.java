package com.coffee.order.domain.cart.service;

import com.coffee.order.common.exception.BusinessException;
import com.coffee.order.common.exception.ErrorCode;
import com.coffee.order.domain.cart.dto.request.CartCheckoutRequestDto;
import com.coffee.order.domain.cart.dto.request.CartItemRequestDto;
import com.coffee.order.domain.cart.dto.response.CartCheckoutResponseDto;
import com.coffee.order.domain.cart.dto.response.CartResponseDto;
import com.coffee.order.domain.menu.entity.Menu;
import com.coffee.order.domain.menu.repository.MenuRepository;
import com.coffee.order.domain.order.dto.response.OrderCreateResponseDto;
import com.coffee.order.domain.order.entity.OrderStatus;
import com.coffee.order.domain.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    private static final String KIOSK_UUID = "kiosk-uuid-001";
    private static final String CART_KEY = "cart:" + KIOSK_UUID;

    @InjectMocks
    private CartService cartService;

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private HashOperations hashOps;
    @Mock private MenuRepository menuRepository;
    @Mock private OrderService orderService;

    private Menu menu1;
    private Menu menu2;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForHash()).thenReturn(hashOps);

        menu1 = Menu.builder().categoryId(1L).name("아메리카노").price(4500).isVisible(true).build();
        ReflectionTestUtils.setField(menu1, "id", 1L);

        menu2 = Menu.builder().categoryId(1L).name("카페라떼").price(5000).isVisible(true).build();
        ReflectionTestUtils.setField(menu2, "id", 2L);
    }

    // ===== addItem =====

    @Test
    @DisplayName("새 메뉴 장바구니에 추가")
    void addItem_새_아이템_추가() {
        // given
        CartItemRequestDto request = new CartItemRequestDto(1L, 2);
        given(hashOps.get(CART_KEY, "1")).willReturn(null);

        // when
        cartService.addItem(KIOSK_UUID, request);

        // then
        then(hashOps).should().put(CART_KEY, "1", "2");
        then(redisTemplate).should().expire(CART_KEY, 30L, TimeUnit.MINUTES);
    }

    @Test
    @DisplayName("이미 있는 메뉴 수량 누적")
    void addItem_기존_아이템_수량_누적() {
        // given: 기존 수량 3, 추가 수량 2 → 5
        CartItemRequestDto request = new CartItemRequestDto(1L, 2);
        given(hashOps.get(CART_KEY, "1")).willReturn("3");

        // when
        cartService.addItem(KIOSK_UUID, request);

        // then: 3 + 2 = 5
        then(hashOps).should().put(CART_KEY, "1", "5");
    }

    @Test
    @DisplayName("addItem 호출 시 TTL 30분 갱신")
    void addItem_TTL_30분_갱신() {
        // given
        CartItemRequestDto request = new CartItemRequestDto(1L, 1);
        given(hashOps.get(CART_KEY, "1")).willReturn(null);

        // when
        cartService.addItem(KIOSK_UUID, request);

        // then
        then(redisTemplate).should().expire(CART_KEY, 30L, TimeUnit.MINUTES);
    }

    // ===== getCart =====

    @Test
    @DisplayName("빈 장바구니 조회 - 빈 목록 반환")
    void getCart_빈_장바구니() {
        // given
        given(hashOps.entries(CART_KEY)).willReturn(Collections.emptyMap());

        // when
        CartResponseDto response = cartService.getCart(KIOSK_UUID);

        // then
        assertThat(response.kioskUuid()).isEqualTo(KIOSK_UUID);
        assertThat(response.items()).isEmpty();
        assertThat(response.totalAmount()).isEqualTo(0);
    }

    @Test
    @DisplayName("아이템 포함 장바구니 조회 - 금액 합산")
    void getCart_아이템_포함_장바구니() {
        // given: menu1(4500) x2, menu2(5000) x1 → total 14000
        Map<Object, Object> cartData = new HashMap<>();
        cartData.put("1", "2");
        cartData.put("2", "1");

        given(hashOps.entries(CART_KEY)).willReturn(cartData);
        given(menuRepository.findAllById(anyList())).willReturn(List.of(menu1, menu2));

        // when
        CartResponseDto response = cartService.getCart(KIOSK_UUID);

        // then
        assertThat(response.items()).hasSize(2);
        assertThat(response.totalAmount()).isEqualTo(14000);
    }

    @Test
    @DisplayName("장바구니 아이템 단가와 소계 검증")
    void getCart_아이템_단가_소계_검증() {
        // given: menu1(4500) x3
        Map<Object, Object> cartData = new HashMap<>();
        cartData.put("1", "3");

        given(hashOps.entries(CART_KEY)).willReturn(cartData);
        given(menuRepository.findAllById(anyList())).willReturn(List.of(menu1));

        // when
        CartResponseDto response = cartService.getCart(KIOSK_UUID);

        // then
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).price()).isEqualTo(4500);
        assertThat(response.items().get(0).quantity()).isEqualTo(3);
        assertThat(response.items().get(0).subtotal()).isEqualTo(13500);
        assertThat(response.totalAmount()).isEqualTo(13500);
    }

    // ===== removeItem =====

    @Test
    @DisplayName("장바구니 메뉴 삭제 성공")
    void removeItem_성공() {
        // given
        given(hashOps.hasKey(CART_KEY, "1")).willReturn(true);

        // when
        cartService.removeItem(KIOSK_UUID, 1L);

        // then
        then(hashOps).should().delete(CART_KEY, "1");
    }

    @Test
    @DisplayName("장바구니에 없는 메뉴 삭제 - CART_ITEM_NOT_FOUND 예외")
    void removeItem_없는_아이템_예외() {
        // given
        given(hashOps.hasKey(CART_KEY, "99")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> cartService.removeItem(KIOSK_UUID, 99L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CART_ITEM_NOT_FOUND);
    }

    // ===== clearCart =====

    @Test
    @DisplayName("장바구니 전체 비우기")
    void clearCart_성공() {
        // when
        cartService.clearCart(KIOSK_UUID);

        // then
        then(redisTemplate).should().delete(CART_KEY);
    }

    // ===== checkout =====

    @Test
    @DisplayName("빈 장바구니 결제 - CART_EMPTY 예외")
    void checkout_빈_장바구니_예외() {
        // given
        given(hashOps.entries(CART_KEY)).willReturn(Collections.emptyMap());

        CartCheckoutRequestDto request = new CartCheckoutRequestDto("010-1234-5678", 1L, 1L);

        // when & then
        assertThatThrownBy(() -> cartService.checkout(KIOSK_UUID, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CART_EMPTY);
    }

    @Test
    @DisplayName("단일 아이템 결제 성공")
    void checkout_단일_아이템_성공() {
        // given: menu1 x2 → totalPrice=9000
        Map<Object, Object> cartData = new HashMap<>();
        cartData.put("1", "2");
        given(hashOps.entries(CART_KEY)).willReturn(cartData);

        OrderCreateResponseDto orderResponse = new OrderCreateResponseDto(
                100L, "아메리카노", 9000, 1000L, OrderStatus.COMPLETED, LocalDateTime.now()
        );
        given(orderService.order(any())).willReturn(orderResponse);

        CartCheckoutRequestDto request = new CartCheckoutRequestDto("010-1234-5678", 1L, 1L);

        // when
        CartCheckoutResponseDto response = cartService.checkout(KIOSK_UUID, request);

        // then
        assertThat(response.orderIds()).containsExactly(100L);
        assertThat(response.totalAmount()).isEqualTo(9000);
        assertThat(response.remainingPoint()).isEqualTo(1000L);
        then(redisTemplate).should().delete(CART_KEY);
    }

    @Test
    @DisplayName("복수 아이템 결제 성공 - 금액 합산")
    void checkout_복수_아이템_성공() {
        // given: menu1 x1, menu2 x2
        Map<Object, Object> cartData = new HashMap<>();
        cartData.put("1", "1");
        cartData.put("2", "2");
        given(hashOps.entries(CART_KEY)).willReturn(cartData);

        OrderCreateResponseDto response1 = new OrderCreateResponseDto(
                101L, "아메리카노", 4500, 5500L, OrderStatus.COMPLETED, LocalDateTime.now()
        );
        OrderCreateResponseDto response2 = new OrderCreateResponseDto(
                102L, "카페라떼", 10000, 500L, OrderStatus.COMPLETED, LocalDateTime.now()
        );
        given(orderService.order(any())).willReturn(response1, response2);

        CartCheckoutRequestDto request = new CartCheckoutRequestDto("010-1234-5678", 1L, 1L);

        // when
        CartCheckoutResponseDto response = cartService.checkout(KIOSK_UUID, request);

        // then
        assertThat(response.orderIds()).hasSize(2);
        assertThat(response.totalAmount()).isEqualTo(14500);
        then(orderService).should(times(2)).order(any());
        then(redisTemplate).should().delete(CART_KEY);
    }

    @Test
    @DisplayName("결제 중 주문 실패 시 예외 전파 - 장바구니 유지")
    void checkout_주문_실패시_예외_전파() {
        // given
        Map<Object, Object> cartData = new HashMap<>();
        cartData.put("1", "1");
        given(hashOps.entries(CART_KEY)).willReturn(cartData);
        given(orderService.order(any())).willThrow(new BusinessException(ErrorCode.INSUFFICIENT_POINT));

        CartCheckoutRequestDto request = new CartCheckoutRequestDto("010-1234-5678", 1L, 1L);

        // when & then: 예외가 전파되고, 장바구니 delete는 호출되지 않음
        assertThatThrownBy(() -> cartService.checkout(KIOSK_UUID, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INSUFFICIENT_POINT);

        then(redisTemplate).should(never()).delete(CART_KEY);
    }

    @Test
    @DisplayName("결제 성공 후 장바구니 삭제")
    void checkout_성공후_장바구니_삭제() {
        // given
        Map<Object, Object> cartData = new HashMap<>();
        cartData.put("1", "1");
        given(hashOps.entries(CART_KEY)).willReturn(cartData);

        OrderCreateResponseDto orderResponse = new OrderCreateResponseDto(
                100L, "아메리카노", 4500, 5500L, OrderStatus.COMPLETED, LocalDateTime.now()
        );
        given(orderService.order(any())).willReturn(orderResponse);

        CartCheckoutRequestDto request = new CartCheckoutRequestDto("010-1234-5678", 1L, 1L);

        // when
        cartService.checkout(KIOSK_UUID, request);

        // then
        then(redisTemplate).should(times(1)).delete(CART_KEY);
    }
}