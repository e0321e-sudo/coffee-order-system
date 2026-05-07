package com.coffee.order.domain.order.service;

import com.coffee.order.common.exception.BusinessException;
import com.coffee.order.common.exception.ErrorCode;
import com.coffee.order.domain.menu.entity.Menu;
import com.coffee.order.domain.menu.entity.MenuStock;
import com.coffee.order.domain.menu.repository.MenuRepository;
import com.coffee.order.domain.menu.repository.MenuStockRepository;
import com.coffee.order.domain.order.dto.request.OrderCreateRequestDto;
import com.coffee.order.domain.order.dto.response.OrderCancelResponseDto;
import com.coffee.order.domain.order.dto.response.OrderCreateResponseDto;
import com.coffee.order.domain.order.entity.Order;
import com.coffee.order.domain.order.entity.OrderStatus;
import com.coffee.order.domain.order.kafka.OrderProducer;
import com.coffee.order.domain.order.repository.OrderRepository;
import com.coffee.order.domain.stock.kafka.StockProducer;
import com.coffee.order.domain.stock.repository.StockHistoryRepository;
import com.coffee.order.domain.store.entity.SpecialClose;
import com.coffee.order.domain.store.entity.Store;
import com.coffee.order.domain.store.repository.SpecialCloseRepository;
import com.coffee.order.domain.store.repository.StoreRepository;
import com.coffee.order.domain.user.entity.User;
import com.coffee.order.domain.user.repository.UserRepository;
import com.coffee.order.domain.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.CALLS_REAL_METHODS;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    // MockedStatic 컨텍스트 진입 전 사전 계산 — try 블록 안에서 LocalTime.of() 호출 시 Mockito 상태 충돌 방지
    private static final LocalTime TIME_10AM         = LocalTime.of(10, 0);
    private static final LocalTime TIME_BEFORE_OPEN  = LocalTime.of(8, 59);
    private static final LocalTime TIME_AFTER_CLOSE  = LocalTime.of(22, 31);
    private static final LocalTime TIME_MIDNIGHT     = LocalTime.of(0, 0);
    private static final LocalTime TIME_OPEN_EXACT   = LocalTime.of(9, 0);
    private static final LocalTime TIME_CLOSE_EXACT  = LocalTime.of(22, 30);

    @InjectMocks
    private OrderService orderService;

    @Mock private UserService userService;
    @Mock private UserRepository userRepository;
    @Mock private MenuRepository menuRepository;
    @Mock private MenuStockRepository menuStockRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private StoreRepository storeRepository;
    @Mock private SpecialCloseRepository specialCloseRepository;
    @Mock private StockHistoryRepository stockHistoryRepository;
    @Mock private OrderProducer orderProducer;
    @Mock private StockProducer stockProducer;

    private OrderCreateRequestDto request;
    private Store activeStore;
    private Menu menu;
    private MenuStock menuStock;
    private User user;
    private Order savedOrder;

    @BeforeEach
    void setUp() {
        request = new OrderCreateRequestDto("01012345678", 1L, 1L, 1L);

        activeStore = Store.builder()
                .name("테스트 커피 매장")
                .address("서울시 강남구")
                .isActive(true)
                .build();

        menu = Menu.builder()
                .categoryId(1L)
                .name("아메리카노")
                .price(4500)
                .isVisible(true)
                .build();
        ReflectionTestUtils.setField(menu, "id", 1L);

        menuStock = MenuStock.builder()
                .storeId(1L)
                .menuId(1L)
                .stock(20)
                .build();

        user = User.builder()
                .phoneNumber("01012345678")
                .point(10000L)
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);

        savedOrder = Order.builder()
                .userId(1L)
                .menuId(1L)
                .storeId(1L)
                .kioskId(1L)
                .totalPrice(4500)
                .build();
        savedOrder.complete();
        ReflectionTestUtils.setField(savedOrder, "id", 100L);
    }

    // ===== order() 성공 케이스 =====

    @Test
    @DisplayName("주문 생성 성공 - 정상 조건 모두 충족")
    void 주문_생성_성공() {
        try (MockedStatic<LocalTime> mockedTime = mockStatic(LocalTime.class, CALLS_REAL_METHODS)) {
            // given
            mockedTime.when(LocalTime::now).thenReturn(TIME_10AM);

            given(storeRepository.findById(1L)).willReturn(Optional.of(activeStore));
            given(specialCloseRepository.findByStoreId(1L)).willReturn(Collections.emptyList());
            given(menuRepository.findByIdAndIsVisibleTrue(1L)).willReturn(Optional.of(menu));
            given(menuStockRepository.findByStoreIdAndMenuIdWithLock(1L, 1L)).willReturn(Optional.of(menuStock));
            given(userService.findOrCreateUser("01012345678")).willReturn(user);
            given(userRepository.findByIdWithLock(1L)).willReturn(Optional.of(user));
            given(orderRepository.save(any(Order.class))).willReturn(savedOrder);
            given(stockHistoryRepository.save(any())).willReturn(null);

            // when
            OrderCreateResponseDto response = orderService.order(request);

            // then
            assertThat(response.menuName()).isEqualTo("아메리카노");
            assertThat(response.totalPrice()).isEqualTo(4500);
            assertThat(response.status()).isEqualTo(OrderStatus.COMPLETED);
        }
    }

    @Test
    @DisplayName("주문 성공 시 포인트 차감 검증")
    void 주문_성공시_포인트_차감_검증() {
        try (MockedStatic<LocalTime> mockedTime = mockStatic(LocalTime.class, CALLS_REAL_METHODS)) {
            // given
            mockedTime.when(LocalTime::now).thenReturn(TIME_10AM);

            given(storeRepository.findById(1L)).willReturn(Optional.of(activeStore));
            given(specialCloseRepository.findByStoreId(1L)).willReturn(Collections.emptyList());
            given(menuRepository.findByIdAndIsVisibleTrue(1L)).willReturn(Optional.of(menu));
            given(menuStockRepository.findByStoreIdAndMenuIdWithLock(1L, 1L)).willReturn(Optional.of(menuStock));
            given(userService.findOrCreateUser("01012345678")).willReturn(user);
            given(userRepository.findByIdWithLock(1L)).willReturn(Optional.of(user));
            given(orderRepository.save(any(Order.class))).willReturn(savedOrder);
            given(stockHistoryRepository.save(any())).willReturn(null);

            // when
            orderService.order(request);

            // then: 10000 - 4500 = 5500
            assertThat(user.getPoint()).isEqualTo(5500L);
        }
    }

    @Test
    @DisplayName("주문 성공 시 재고 차감 검증")
    void 주문_성공시_재고_차감_검증() {
        try (MockedStatic<LocalTime> mockedTime = mockStatic(LocalTime.class, CALLS_REAL_METHODS)) {
            // given
            mockedTime.when(LocalTime::now).thenReturn(TIME_10AM);

            given(storeRepository.findById(1L)).willReturn(Optional.of(activeStore));
            given(specialCloseRepository.findByStoreId(1L)).willReturn(Collections.emptyList());
            given(menuRepository.findByIdAndIsVisibleTrue(1L)).willReturn(Optional.of(menu));
            given(menuStockRepository.findByStoreIdAndMenuIdWithLock(1L, 1L)).willReturn(Optional.of(menuStock));
            given(userService.findOrCreateUser("01012345678")).willReturn(user);
            given(userRepository.findByIdWithLock(1L)).willReturn(Optional.of(user));
            given(orderRepository.save(any(Order.class))).willReturn(savedOrder);
            given(stockHistoryRepository.save(any())).willReturn(null);

            // when
            orderService.order(request);

            // then: 20 - 1 = 19
            assertThat(menuStock.getStock()).isEqualTo(19);
        }
    }

    @Test
    @DisplayName("주문 성공 시 재고 변동 이력 저장")
    void 주문_성공시_재고이력_저장() {
        try (MockedStatic<LocalTime> mockedTime = mockStatic(LocalTime.class, CALLS_REAL_METHODS)) {
            // given
            mockedTime.when(LocalTime::now).thenReturn(TIME_10AM);

            given(storeRepository.findById(1L)).willReturn(Optional.of(activeStore));
            given(specialCloseRepository.findByStoreId(1L)).willReturn(Collections.emptyList());
            given(menuRepository.findByIdAndIsVisibleTrue(1L)).willReturn(Optional.of(menu));
            given(menuStockRepository.findByStoreIdAndMenuIdWithLock(1L, 1L)).willReturn(Optional.of(menuStock));
            given(userService.findOrCreateUser("01012345678")).willReturn(user);
            given(userRepository.findByIdWithLock(1L)).willReturn(Optional.of(user));
            given(orderRepository.save(any(Order.class))).willReturn(savedOrder);
            given(stockHistoryRepository.save(any())).willReturn(null);

            // when
            orderService.order(request);

            // then
            then(stockHistoryRepository).should(times(1)).save(any());
        }
    }

    @Test
    @DisplayName("주문 성공 시 Kafka 주문 완료 이벤트 발행")
    void 주문_성공시_카프카_이벤트_발행() {
        try (MockedStatic<LocalTime> mockedTime = mockStatic(LocalTime.class, CALLS_REAL_METHODS)) {
            // given
            mockedTime.when(LocalTime::now).thenReturn(TIME_10AM);

            given(storeRepository.findById(1L)).willReturn(Optional.of(activeStore));
            given(specialCloseRepository.findByStoreId(1L)).willReturn(Collections.emptyList());
            given(menuRepository.findByIdAndIsVisibleTrue(1L)).willReturn(Optional.of(menu));
            given(menuStockRepository.findByStoreIdAndMenuIdWithLock(1L, 1L)).willReturn(Optional.of(menuStock));
            given(userService.findOrCreateUser("01012345678")).willReturn(user);
            given(userRepository.findByIdWithLock(1L)).willReturn(Optional.of(user));
            given(orderRepository.save(any(Order.class))).willReturn(savedOrder);
            given(stockHistoryRepository.save(any())).willReturn(null);

            // when
            orderService.order(request);

            // then
            then(orderProducer).should(times(1)).sendOrderCompleted(any());
        }
    }

    // ===== order() 영업시간 체크 =====

    @Test
    @DisplayName("영업 시작(09:00) 이전 주문 - OUT_OF_BUSINESS_HOURS 예외")
    void 주문_영업시간_이전_예외() {
        try (MockedStatic<LocalTime> mockedTime = mockStatic(LocalTime.class, CALLS_REAL_METHODS)) {
            // given: 08:59
            mockedTime.when(LocalTime::now).thenReturn(TIME_BEFORE_OPEN);

            // when & then
            assertThatThrownBy(() -> orderService.order(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.OUT_OF_BUSINESS_HOURS);
        }
    }

    @Test
    @DisplayName("영업 종료(22:30) 이후 주문 - OUT_OF_BUSINESS_HOURS 예외")
    void 주문_영업시간_이후_예외() {
        try (MockedStatic<LocalTime> mockedTime = mockStatic(LocalTime.class, CALLS_REAL_METHODS)) {
            // given: 22:31
            mockedTime.when(LocalTime::now).thenReturn(TIME_AFTER_CLOSE);

            // when & then
            assertThatThrownBy(() -> orderService.order(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.OUT_OF_BUSINESS_HOURS);
        }
    }

    @Test
    @DisplayName("자정(00:00) 주문 - OUT_OF_BUSINESS_HOURS 예외")
    void 주문_자정_예외() {
        try (MockedStatic<LocalTime> mockedTime = mockStatic(LocalTime.class, CALLS_REAL_METHODS)) {
            // given: 00:00
            mockedTime.when(LocalTime::now).thenReturn(TIME_MIDNIGHT);

            // when & then
            assertThatThrownBy(() -> orderService.order(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.OUT_OF_BUSINESS_HOURS);
        }
    }

    @Test
    @DisplayName("영업 시작 정각(09:00) 주문 성공 - 경계값")
    void 주문_영업시작_정각_성공() {
        try (MockedStatic<LocalTime> mockedTime = mockStatic(LocalTime.class, CALLS_REAL_METHODS)) {
            // given: 09:00 정각 (isBefore(09:00) = false → 통과)
            mockedTime.when(LocalTime::now).thenReturn(TIME_OPEN_EXACT);

            given(storeRepository.findById(1L)).willReturn(Optional.of(activeStore));
            given(specialCloseRepository.findByStoreId(1L)).willReturn(Collections.emptyList());
            given(menuRepository.findByIdAndIsVisibleTrue(1L)).willReturn(Optional.of(menu));
            given(menuStockRepository.findByStoreIdAndMenuIdWithLock(1L, 1L)).willReturn(Optional.of(menuStock));
            given(userService.findOrCreateUser("01012345678")).willReturn(user);
            given(userRepository.findByIdWithLock(1L)).willReturn(Optional.of(user));
            given(orderRepository.save(any(Order.class))).willReturn(savedOrder);
            given(stockHistoryRepository.save(any())).willReturn(null);

            // when & then: 예외 없이 통과
            assertThatCode(() -> orderService.order(request)).doesNotThrowAnyException();
        }
    }

    @Test
    @DisplayName("영업 종료 정각(22:30) 주문 성공 - 경계값")
    void 주문_영업종료_정각_성공() {
        try (MockedStatic<LocalTime> mockedTime = mockStatic(LocalTime.class, CALLS_REAL_METHODS)) {
            // given: 22:30 정각 (isAfter(22:30) = false → 통과)
            mockedTime.when(LocalTime::now).thenReturn(TIME_CLOSE_EXACT);

            given(storeRepository.findById(1L)).willReturn(Optional.of(activeStore));
            given(specialCloseRepository.findByStoreId(1L)).willReturn(Collections.emptyList());
            given(menuRepository.findByIdAndIsVisibleTrue(1L)).willReturn(Optional.of(menu));
            given(menuStockRepository.findByStoreIdAndMenuIdWithLock(1L, 1L)).willReturn(Optional.of(menuStock));
            given(userService.findOrCreateUser("01012345678")).willReturn(user);
            given(userRepository.findByIdWithLock(1L)).willReturn(Optional.of(user));
            given(orderRepository.save(any(Order.class))).willReturn(savedOrder);
            given(stockHistoryRepository.save(any())).willReturn(null);

            // when & then: 예외 없이 통과
            assertThatCode(() -> orderService.order(request)).doesNotThrowAnyException();
        }
    }

    // ===== order() 매장/임시휴무 체크 =====

    @Test
    @DisplayName("존재하지 않는 매장 주문 - STORE_CLOSED 예외")
    void 주문_매장없음_예외() {
        try (MockedStatic<LocalTime> mockedTime = mockStatic(LocalTime.class, CALLS_REAL_METHODS)) {
            // given
            mockedTime.when(LocalTime::now).thenReturn(TIME_10AM);
            given(storeRepository.findById(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> orderService.order(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.STORE_CLOSED);
        }
    }

    @Test
    @DisplayName("비활성(폐업) 매장 주문 - STORE_CLOSED 예외")
    void 주문_비활성매장_예외() {
        try (MockedStatic<LocalTime> mockedTime = mockStatic(LocalTime.class, CALLS_REAL_METHODS)) {
            // given
            mockedTime.when(LocalTime::now).thenReturn(TIME_10AM);

            Store inactiveStore = Store.builder()
                    .name("휴업 매장")
                    .address("서울시 강남구")
                    .isActive(false)
                    .build();
            given(storeRepository.findById(1L)).willReturn(Optional.of(inactiveStore));
            given(specialCloseRepository.findByStoreId(1L)).willReturn(Collections.emptyList());

            // when & then
            assertThatThrownBy(() -> orderService.order(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.STORE_CLOSED);
        }
    }

    @Test
    @DisplayName("오늘 날짜 임시 휴무 등록된 매장 주문 - STORE_CLOSED 예외")
    void 주문_오늘_임시휴무_예외() {
        try (MockedStatic<LocalTime> mockedTime = mockStatic(LocalTime.class, CALLS_REAL_METHODS)) {
            // given
            mockedTime.when(LocalTime::now).thenReturn(TIME_10AM);

            SpecialClose todayClose = SpecialClose.builder()
                    .storeId(1L)
                    .closeDate(LocalDate.now())
                    .reason("임시 휴무")
                    .build();
            given(storeRepository.findById(1L)).willReturn(Optional.of(activeStore));
            given(specialCloseRepository.findByStoreId(1L)).willReturn(List.of(todayClose));

            // when & then
            assertThatThrownBy(() -> orderService.order(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.STORE_CLOSED);
        }
    }

    @Test
    @DisplayName("내일 날짜 임시 휴무 등록 - 오늘 주문 성공")
    void 주문_내일_임시휴무_오늘주문_성공() {
        try (MockedStatic<LocalTime> mockedTime = mockStatic(LocalTime.class, CALLS_REAL_METHODS)) {
            // given: 내일 날짜의 임시 휴무 → 오늘은 영업
            mockedTime.when(LocalTime::now).thenReturn(TIME_10AM);

            SpecialClose tomorrowClose = SpecialClose.builder()
                    .storeId(1L)
                    .closeDate(LocalDate.now().plusDays(1))
                    .reason("내일 임시 휴무")
                    .build();
            given(storeRepository.findById(1L)).willReturn(Optional.of(activeStore));
            given(specialCloseRepository.findByStoreId(1L)).willReturn(List.of(tomorrowClose));
            given(menuRepository.findByIdAndIsVisibleTrue(1L)).willReturn(Optional.of(menu));
            given(menuStockRepository.findByStoreIdAndMenuIdWithLock(1L, 1L)).willReturn(Optional.of(menuStock));
            given(userService.findOrCreateUser("01012345678")).willReturn(user);
            given(userRepository.findByIdWithLock(1L)).willReturn(Optional.of(user));
            given(orderRepository.save(any(Order.class))).willReturn(savedOrder);
            given(stockHistoryRepository.save(any())).willReturn(null);

            // when & then: 예외 없이 통과
            assertThatCode(() -> orderService.order(request)).doesNotThrowAnyException();
        }
    }

    // ===== order() 메뉴/재고 체크 =====

    @Test
    @DisplayName("존재하지 않는 메뉴 주문 - MENU_NOT_FOUND 예외")
    void 주문_메뉴없음_예외() {
        try (MockedStatic<LocalTime> mockedTime = mockStatic(LocalTime.class, CALLS_REAL_METHODS)) {
            // given
            mockedTime.when(LocalTime::now).thenReturn(TIME_10AM);

            given(storeRepository.findById(1L)).willReturn(Optional.of(activeStore));
            given(specialCloseRepository.findByStoreId(1L)).willReturn(Collections.emptyList());
            given(menuRepository.findByIdAndIsVisibleTrue(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> orderService.order(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.MENU_NOT_FOUND);
        }
    }

    @Test
    @DisplayName("재고 레코드 없음 - MENU_SOLD_OUT 예외")
    void 주문_재고레코드없음_예외() {
        try (MockedStatic<LocalTime> mockedTime = mockStatic(LocalTime.class, CALLS_REAL_METHODS)) {
            // given
            mockedTime.when(LocalTime::now).thenReturn(TIME_10AM);

            given(storeRepository.findById(1L)).willReturn(Optional.of(activeStore));
            given(specialCloseRepository.findByStoreId(1L)).willReturn(Collections.emptyList());
            given(menuRepository.findByIdAndIsVisibleTrue(1L)).willReturn(Optional.of(menu));
            given(menuStockRepository.findByStoreIdAndMenuIdWithLock(1L, 1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> orderService.order(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.MENU_SOLD_OUT);
        }
    }

    @Test
    @DisplayName("품절 메뉴 주문 - MENU_SOLD_OUT 예외")
    void 주문_품절메뉴_예외() {
        try (MockedStatic<LocalTime> mockedTime = mockStatic(LocalTime.class, CALLS_REAL_METHODS)) {
            // given
            mockedTime.when(LocalTime::now).thenReturn(TIME_10AM);

            MenuStock soldOutStock = MenuStock.builder()
                    .storeId(1L)
                    .menuId(1L)
                    .stock(0)
                    .build();
            given(storeRepository.findById(1L)).willReturn(Optional.of(activeStore));
            given(specialCloseRepository.findByStoreId(1L)).willReturn(Collections.emptyList());
            given(menuRepository.findByIdAndIsVisibleTrue(1L)).willReturn(Optional.of(menu));
            given(menuStockRepository.findByStoreIdAndMenuIdWithLock(1L, 1L)).willReturn(Optional.of(soldOutStock));

            // when & then
            assertThatThrownBy(() -> orderService.order(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.MENU_SOLD_OUT);
        }
    }

    // ===== order() 포인트 체크 =====

    @Test
    @DisplayName("포인트 부족 주문 - INSUFFICIENT_POINT 예외")
    void 주문_포인트부족_예외() {
        try (MockedStatic<LocalTime> mockedTime = mockStatic(LocalTime.class, CALLS_REAL_METHODS)) {
            // given: 포인트 1000, 메뉴 가격 4500
            mockedTime.when(LocalTime::now).thenReturn(TIME_10AM);

            User poorUser = User.builder()
                    .phoneNumber("01012345678")
                    .point(1000L)
                    .build();
            ReflectionTestUtils.setField(poorUser, "id", 1L);

            given(storeRepository.findById(1L)).willReturn(Optional.of(activeStore));
            given(specialCloseRepository.findByStoreId(1L)).willReturn(Collections.emptyList());
            given(menuRepository.findByIdAndIsVisibleTrue(1L)).willReturn(Optional.of(menu));
            given(menuStockRepository.findByStoreIdAndMenuIdWithLock(1L, 1L)).willReturn(Optional.of(menuStock));
            given(userService.findOrCreateUser("01012345678")).willReturn(poorUser);
            given(userRepository.findByIdWithLock(1L)).willReturn(Optional.of(poorUser));

            // when & then
            assertThatThrownBy(() -> orderService.order(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INSUFFICIENT_POINT);
        }
    }

    @Test
    @DisplayName("포인트 1원 부족 - INSUFFICIENT_POINT 예외 - 경계값")
    void 주문_포인트_1원부족_예외() {
        try (MockedStatic<LocalTime> mockedTime = mockStatic(LocalTime.class, CALLS_REAL_METHODS)) {
            // given: 포인트 4499, 메뉴 가격 4500 (1원 부족)
            mockedTime.when(LocalTime::now).thenReturn(TIME_10AM);

            User almostUser = User.builder()
                    .phoneNumber("01012345678")
                    .point(4499L)
                    .build();
            ReflectionTestUtils.setField(almostUser, "id", 1L);

            given(storeRepository.findById(1L)).willReturn(Optional.of(activeStore));
            given(specialCloseRepository.findByStoreId(1L)).willReturn(Collections.emptyList());
            given(menuRepository.findByIdAndIsVisibleTrue(1L)).willReturn(Optional.of(menu));
            given(menuStockRepository.findByStoreIdAndMenuIdWithLock(1L, 1L)).willReturn(Optional.of(menuStock));
            given(userService.findOrCreateUser("01012345678")).willReturn(almostUser);
            given(userRepository.findByIdWithLock(1L)).willReturn(Optional.of(almostUser));

            // when & then
            assertThatThrownBy(() -> orderService.order(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INSUFFICIENT_POINT);
        }
    }

    @Test
    @DisplayName("포인트 정확히 일치 - 주문 성공 경계값")
    void 주문_포인트_정확히_일치_성공() {
        try (MockedStatic<LocalTime> mockedTime = mockStatic(LocalTime.class, CALLS_REAL_METHODS)) {
            // given: 포인트 = 메뉴 가격 4500
            mockedTime.when(LocalTime::now).thenReturn(TIME_10AM);

            User exactUser = User.builder()
                    .phoneNumber("01012345678")
                    .point(4500L)
                    .build();
            ReflectionTestUtils.setField(exactUser, "id", 1L);

            given(storeRepository.findById(1L)).willReturn(Optional.of(activeStore));
            given(specialCloseRepository.findByStoreId(1L)).willReturn(Collections.emptyList());
            given(menuRepository.findByIdAndIsVisibleTrue(1L)).willReturn(Optional.of(menu));
            given(menuStockRepository.findByStoreIdAndMenuIdWithLock(1L, 1L)).willReturn(Optional.of(menuStock));
            given(userService.findOrCreateUser("01012345678")).willReturn(exactUser);
            given(userRepository.findByIdWithLock(1L)).willReturn(Optional.of(exactUser));
            given(orderRepository.save(any(Order.class))).willReturn(savedOrder);
            given(stockHistoryRepository.save(any())).willReturn(null);

            // when
            orderService.order(request);

            // then: 잔액 0원
            assertThat(exactUser.getPoint()).isEqualTo(0L);
        }
    }

    // ===== order() 재고 부족 알림 =====

    @Test
    @DisplayName("재고 차감 후 10개 이하 - 재고 부족 알림 발행")
    void 주문_재고부족알림_발행() {
        try (MockedStatic<LocalTime> mockedTime = mockStatic(LocalTime.class, CALLS_REAL_METHODS)) {
            // given: stock=11, 차감 후 10 → 10 <= LOW_STOCK_THRESHOLD(10) → 알림 발행
            mockedTime.when(LocalTime::now).thenReturn(TIME_10AM);

            MenuStock lowStock = MenuStock.builder()
                    .storeId(1L)
                    .menuId(1L)
                    .stock(11)
                    .build();
            given(storeRepository.findById(1L)).willReturn(Optional.of(activeStore));
            given(specialCloseRepository.findByStoreId(1L)).willReturn(Collections.emptyList());
            given(menuRepository.findByIdAndIsVisibleTrue(1L)).willReturn(Optional.of(menu));
            given(menuStockRepository.findByStoreIdAndMenuIdWithLock(1L, 1L)).willReturn(Optional.of(lowStock));
            given(userService.findOrCreateUser("01012345678")).willReturn(user);
            given(userRepository.findByIdWithLock(1L)).willReturn(Optional.of(user));
            given(orderRepository.save(any(Order.class))).willReturn(savedOrder);
            given(stockHistoryRepository.save(any())).willReturn(null);

            // when
            orderService.order(request);

            // then: 재고 부족 알림 1회 발행
            then(stockProducer).should(times(1)).sendStockAlert(any());
        }
    }

    @Test
    @DisplayName("재고 차감 후 11개 초과 - 재고 부족 알림 미발행")
    void 주문_재고충분_알림_미발행() {
        try (MockedStatic<LocalTime> mockedTime = mockStatic(LocalTime.class, CALLS_REAL_METHODS)) {
            // given: stock=12, 차감 후 11 → 11 > LOW_STOCK_THRESHOLD(10) → 알림 미발행
            mockedTime.when(LocalTime::now).thenReturn(TIME_10AM);

            MenuStock sufficientStock = MenuStock.builder()
                    .storeId(1L)
                    .menuId(1L)
                    .stock(12)
                    .build();
            given(storeRepository.findById(1L)).willReturn(Optional.of(activeStore));
            given(specialCloseRepository.findByStoreId(1L)).willReturn(Collections.emptyList());
            given(menuRepository.findByIdAndIsVisibleTrue(1L)).willReturn(Optional.of(menu));
            given(menuStockRepository.findByStoreIdAndMenuIdWithLock(1L, 1L)).willReturn(Optional.of(sufficientStock));
            given(userService.findOrCreateUser("01012345678")).willReturn(user);
            given(userRepository.findByIdWithLock(1L)).willReturn(Optional.of(user));
            given(orderRepository.save(any(Order.class))).willReturn(savedOrder);
            given(stockHistoryRepository.save(any())).willReturn(null);

            // when
            orderService.order(request);

            // then: 재고 부족 알림 미발행
            then(stockProducer).should(never()).sendStockAlert(any());
        }
    }

    // ===== cancel() 성공 케이스 =====

    @Test
    @DisplayName("주문 취소 성공 - CANCELLED 상태 반환")
    void 주문_취소_성공() {
        // given
        Order order = Order.builder()
                .userId(1L)
                .menuId(1L)
                .storeId(1L)
                .kioskId(1L)
                .totalPrice(4500)
                .build();
        order.complete();
        ReflectionTestUtils.setField(order, "id", 100L);

        given(userRepository.findByPhoneNumber("01012345678")).willReturn(Optional.of(user));
        given(orderRepository.findByIdAndUserId(100L, 1L)).willReturn(Optional.of(order));
        given(userRepository.findByIdWithLock(1L)).willReturn(Optional.of(user));
        given(menuStockRepository.findByStoreIdAndMenuIdWithLock(1L, 1L)).willReturn(Optional.of(menuStock));
        given(stockHistoryRepository.save(any())).willReturn(null);

        // when
        OrderCancelResponseDto response = orderService.cancel(100L, "01012345678");

        // then
        assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(response.refundedPoint()).isEqualTo(4500L);
    }

    @Test
    @DisplayName("주문 취소 시 포인트 환불")
    void 주문_취소시_포인트_환불() {
        // given
        Order order = Order.builder()
                .userId(1L)
                .menuId(1L)
                .storeId(1L)
                .kioskId(1L)
                .totalPrice(4500)
                .build();
        order.complete();
        ReflectionTestUtils.setField(order, "id", 100L);

        User lockedUser = User.builder()
                .phoneNumber("01012345678")
                .point(5500L)
                .build();
        ReflectionTestUtils.setField(lockedUser, "id", 1L);

        given(userRepository.findByPhoneNumber("01012345678")).willReturn(Optional.of(user));
        given(orderRepository.findByIdAndUserId(100L, 1L)).willReturn(Optional.of(order));
        given(userRepository.findByIdWithLock(1L)).willReturn(Optional.of(lockedUser));
        given(menuStockRepository.findByStoreIdAndMenuIdWithLock(1L, 1L)).willReturn(Optional.of(menuStock));
        given(stockHistoryRepository.save(any())).willReturn(null);

        // when
        orderService.cancel(100L, "01012345678");

        // then: 5500 + 4500 = 10000
        assertThat(lockedUser.getPoint()).isEqualTo(10000L);
    }

    @Test
    @DisplayName("주문 취소 시 재고 복구")
    void 주문_취소시_재고_복구() {
        // given
        MenuStock depleted = MenuStock.builder()
                .storeId(1L)
                .menuId(1L)
                .stock(9)
                .build();

        Order order = Order.builder()
                .userId(1L)
                .menuId(1L)
                .storeId(1L)
                .kioskId(1L)
                .totalPrice(4500)
                .build();
        order.complete();
        ReflectionTestUtils.setField(order, "id", 100L);

        given(userRepository.findByPhoneNumber("01012345678")).willReturn(Optional.of(user));
        given(orderRepository.findByIdAndUserId(100L, 1L)).willReturn(Optional.of(order));
        given(userRepository.findByIdWithLock(1L)).willReturn(Optional.of(user));
        given(menuStockRepository.findByStoreIdAndMenuIdWithLock(1L, 1L)).willReturn(Optional.of(depleted));
        given(stockHistoryRepository.save(any())).willReturn(null);

        // when
        orderService.cancel(100L, "01012345678");

        // then: 9 + 1 = 10
        assertThat(depleted.getStock()).isEqualTo(10);
    }

    @Test
    @DisplayName("주문 취소 시 재고 변동 이력 저장")
    void 주문_취소시_재고이력_저장() {
        // given
        Order order = Order.builder()
                .userId(1L)
                .menuId(1L)
                .storeId(1L)
                .kioskId(1L)
                .totalPrice(4500)
                .build();
        order.complete();
        ReflectionTestUtils.setField(order, "id", 100L);

        given(userRepository.findByPhoneNumber("01012345678")).willReturn(Optional.of(user));
        given(orderRepository.findByIdAndUserId(100L, 1L)).willReturn(Optional.of(order));
        given(userRepository.findByIdWithLock(1L)).willReturn(Optional.of(user));
        given(menuStockRepository.findByStoreIdAndMenuIdWithLock(1L, 1L)).willReturn(Optional.of(menuStock));
        given(stockHistoryRepository.save(any())).willReturn(null);

        // when
        orderService.cancel(100L, "01012345678");

        // then
        then(stockHistoryRepository).should(times(1)).save(any());
    }

    // ===== cancel() 실패 케이스 =====

    @Test
    @DisplayName("취소 - 존재하지 않는 사용자 USER_NOT_FOUND 예외")
    void 취소_사용자없음_예외() {
        // given
        given(userRepository.findByPhoneNumber("01099999999")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> orderService.cancel(100L, "01099999999"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("취소 - 다른 사용자의 주문 ORDER_NOT_FOUND 예외")
    void 취소_타인주문_예외() {
        // given
        given(userRepository.findByPhoneNumber("01012345678")).willReturn(Optional.of(user));
        given(orderRepository.findByIdAndUserId(999L, 1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> orderService.cancel(999L, "01012345678"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ORDER_NOT_FOUND);
    }

    @Test
    @DisplayName("취소 - 존재하지 않는 주문 ORDER_NOT_FOUND 예외")
    void 취소_주문없음_예외() {
        // given
        given(userRepository.findByPhoneNumber("01012345678")).willReturn(Optional.of(user));
        given(orderRepository.findByIdAndUserId(100L, 1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> orderService.cancel(100L, "01012345678"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ORDER_NOT_FOUND);
    }

    @Test
    @DisplayName("취소 - 5분 초과된 주문 ORDER_CANCEL_EXPIRED 예외")
    void 취소_5분초과_예외() {
        // given: 10분 전 주문
        Order expiredOrder = Order.builder()
                .userId(1L)
                .menuId(1L)
                .storeId(1L)
                .kioskId(1L)
                .totalPrice(4500)
                .build();
        ReflectionTestUtils.setField(expiredOrder, "createdAt",
                LocalDateTime.now().minusMinutes(10));

        given(userRepository.findByPhoneNumber("01012345678")).willReturn(Optional.of(user));
        given(orderRepository.findByIdAndUserId(100L, 1L)).willReturn(Optional.of(expiredOrder));

        // when & then
        assertThatThrownBy(() -> orderService.cancel(100L, "01012345678"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ORDER_CANCEL_EXPIRED);
    }

    @Test
    @DisplayName("취소 - 정확히 5분 1초 경과 ORDER_CANCEL_EXPIRED 예외 - 경계값")
    void 취소_5분1초_경과_예외() {
        // given: 5분 1초 전 주문
        Order borderOrder = Order.builder()
                .userId(1L)
                .menuId(1L)
                .storeId(1L)
                .kioskId(1L)
                .totalPrice(4500)
                .build();
        ReflectionTestUtils.setField(borderOrder, "createdAt",
                LocalDateTime.now().minusMinutes(5).minusSeconds(1));

        given(userRepository.findByPhoneNumber("01012345678")).willReturn(Optional.of(user));
        given(orderRepository.findByIdAndUserId(100L, 1L)).willReturn(Optional.of(borderOrder));

        // when & then
        assertThatThrownBy(() -> orderService.cancel(100L, "01012345678"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ORDER_CANCEL_EXPIRED);
    }
}