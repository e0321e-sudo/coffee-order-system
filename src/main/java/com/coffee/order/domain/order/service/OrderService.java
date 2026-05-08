package com.coffee.order.domain.order.service;

import com.coffee.order.common.exception.BusinessException;
import com.coffee.order.common.exception.ErrorCode;
import com.coffee.order.common.service.IdempotencyService;
import com.coffee.order.domain.menu.entity.Menu;
import com.coffee.order.domain.menu.entity.MenuStock;
import com.coffee.order.domain.menu.repository.MenuRepository;
import com.coffee.order.domain.menu.repository.MenuStockRepository;
import com.coffee.order.domain.order.dto.event.OrderCompletedEvent;
import com.coffee.order.domain.order.dto.request.OrderCreateRequestDto;
import com.coffee.order.domain.order.dto.response.OrderCancelResponseDto;
import com.coffee.order.domain.order.dto.response.OrderCreateResponseDto;
import com.coffee.order.domain.order.entity.Order;
import com.coffee.order.domain.order.kafka.OrderProducer;
import com.coffee.order.domain.order.repository.OrderRepository;
import com.coffee.order.domain.stock.entity.StockHistory;
import com.coffee.order.domain.stock.entity.StockHistoryType;
import com.coffee.order.domain.stock.kafka.StockProducer;
import com.coffee.order.domain.stock.kafka.event.StockAlertEvent;
import com.coffee.order.domain.stock.repository.StockHistoryRepository;
import com.coffee.order.domain.store.entity.SpecialClose;
import com.coffee.order.domain.store.entity.Store;
import com.coffee.order.domain.store.repository.SpecialCloseRepository;
import com.coffee.order.domain.store.repository.StoreRepository;
import com.coffee.order.domain.user.entity.User;
import com.coffee.order.domain.user.repository.UserRepository;
import com.coffee.order.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final LocalTime OPEN_TIME = LocalTime.of(9, 0);
    private static final LocalTime CLOSE_TIME = LocalTime.of(22, 30);

    private final UserService userService;
    private final UserRepository userRepository;
    private final MenuRepository menuRepository;
    private final MenuStockRepository menuStockRepository;
    private final OrderRepository orderRepository;
    private final StoreRepository storeRepository;
    private final SpecialCloseRepository specialCloseRepository;
    private final StockHistoryRepository stockHistoryRepository;
    private final OrderProducer orderProducer;
    private final StockProducer stockProducer;
    private final IdempotencyService idempotencyService;

    @Transactional
    public OrderCreateResponseDto order(OrderCreateRequestDto request) {

        // 중복 주문 방지 - 같은 Idempotency-Key로 5분 내 재요청 시 차단
        if (request.getIdempotencyKey() != null) {
            idempotencyService.checkAndSave(request.getIdempotencyKey(), "order");
        }
        // 1. 영업시간 체크
        LocalTime now = LocalTime.now();
        if (now.isBefore(OPEN_TIME) || now.isAfter(CLOSE_TIME)) {
            throw new BusinessException(ErrorCode.OUT_OF_BUSINESS_HOURS);
        }

        // 2. 임시휴무 체크
        Store store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() -> new BusinessException(ErrorCode.STORE_CLOSED));
        List<SpecialClose> specialCloses = specialCloseRepository.findByStoreId(request.getStoreId());
        store.validateOpen(LocalDate.now(), specialCloses);

        // 3. 메뉴 조회 및 품절 체크
        Menu menu = menuRepository.findByIdAndIsVisibleTrue(request.getMenuId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MENU_NOT_FOUND));
        MenuStock menuStock = menuStockRepository
                .findByStoreIdAndMenuIdWithLock(request.getStoreId(), request.getMenuId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MENU_SOLD_OUT));
        menuStock.validateNotSoldOut();

        // 4. 비관적 락으로 포인트 차감
        int quantity = request.getQuantity();
        User user = userService.findOrCreateUser(request.getPhoneNumber());
        User lockedUser = userRepository.findByIdWithLock(user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        lockedUser.deductPoint((long) menu.getPrice() * quantity);

        // 5. 재고 차감
        if (menuStock.getStock() < quantity) {
            throw new BusinessException(ErrorCode.MENU_SOLD_OUT);
        }
        int stockBefore = menuStock.getStock();
        for (int i = 0; i < quantity; i++) {
            menuStock.decreaseStock();
        }

        // 재고 10개 이하 감지 시 stock-alert 발행
        if (menuStock.getStock() <= MenuStock.LOW_STOCK_THRESHOLD) {
            stockProducer.sendStockAlert(new StockAlertEvent(
                    request.getStoreId(),
                    store.getName(),
                    menu.getId(),
                    menu.getName(),
                    menuStock.getStock()
            ));
        }

        // 6. 주문 생성 (COMPLETED)
        Order savedOrder = orderRepository.save(
                buildCompletedOrder(lockedUser.getId(), menu, request)
        );

        // 7. 재고 변동 이력 기록
        stockHistoryRepository.save(StockHistory.builder()
                .menuId(menu.getId())
                .storeId(request.getStoreId())
                .type(StockHistoryType.ORDER_USE)
                .changedAmount(quantity)
                .stockBefore(stockBefore)
                .stockAfter(menuStock.getStock())
                .adminId(null)
                .build());

        // 8. Kafka 주문 완료 이벤트 발행
        orderProducer.sendOrderCompleted(new OrderCompletedEvent(
                savedOrder.getId(),
                lockedUser.getId(),
                lockedUser.getPhoneNumber(),
                menu.getId(),
                request.getStoreId(),
                request.getKioskId(),
                savedOrder.getTotalPrice(),
                savedOrder.getCreatedAt()
        ));

        return OrderCreateResponseDto.of(savedOrder, menu.getName(), lockedUser.getPoint());
    }

    @Transactional
    public OrderCancelResponseDto cancel(Long orderId, String phoneNumber) {
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Order order = orderRepository.findByIdAndUserId(orderId, user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        order.validateCancelable();

        // 비관적 락으로 포인트 환불
        User lockedUser = userRepository.findByIdWithLock(user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        lockedUser.chargePoint(order.getTotalPrice());

        // 재고 복구
        MenuStock menuStock = menuStockRepository
                .findByStoreIdAndMenuIdWithLock(order.getStoreId(), order.getMenuId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MENU_NOT_FOUND));
        int stockBefore = menuStock.getStock();
        menuStock.increaseStock();

        // 재고 변동 이력 기록 (ADJUSTMENT)
        stockHistoryRepository.save(StockHistory.builder()
                .menuId(order.getMenuId())
                .storeId(order.getStoreId())
                .type(StockHistoryType.ADJUSTMENT)
                .changedAmount(1)
                .stockBefore(stockBefore)
                .stockAfter(menuStock.getStock())
                .adminId(null)
                .build());

        order.cancel();

        return OrderCancelResponseDto.of(order, order.getTotalPrice(), lockedUser.getPoint());
    }

    private Order buildCompletedOrder(Long userId, Menu menu, OrderCreateRequestDto request) {
        Order order = Order.builder()
                .userId(userId)
                .menuId(menu.getId())
                .storeId(request.getStoreId())
                .kioskId(request.getKioskId())
                .totalPrice(menu.getPrice() * request.getQuantity())
                .build();
        order.complete();
        return order;
    }
}