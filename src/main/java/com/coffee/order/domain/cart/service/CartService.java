package com.coffee.order.domain.cart.service;

import com.coffee.order.common.exception.BusinessException;
import com.coffee.order.common.exception.ErrorCode;
import com.coffee.order.domain.cart.dto.request.CartCheckoutRequestDto;
import com.coffee.order.domain.cart.dto.request.CartItemRequestDto;
import com.coffee.order.domain.cart.dto.response.CartCheckoutResponseDto;
import com.coffee.order.domain.cart.dto.response.CartItemResponseDto;
import com.coffee.order.domain.cart.dto.response.CartResponseDto;
import com.coffee.order.domain.menu.entity.Menu;
import com.coffee.order.domain.menu.repository.MenuRepository;
import com.coffee.order.domain.order.dto.request.OrderCreateRequestDto;
import com.coffee.order.domain.order.dto.response.OrderCreateResponseDto;
import com.coffee.order.domain.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private static final String CART_KEY_PREFIX = "cart:";         // Redis 키 형식: cart:{kioskUuid}
    private static final long CART_TTL_MINUTES = 30;               // 장바구니 유효시간: 30분 (30분 동안 아무 동작 없으면 자동 삭제)

    private final StringRedisTemplate redisTemplate;
    private final MenuRepository menuRepository;
    private final OrderService orderService;

    // 장바구니에 메뉴 추가
    public void addItem(String kioskUuid, CartItemRequestDto request) {
        String key = CART_KEY_PREFIX + kioskUuid;
        String menuId = String.valueOf(request.getMenuId());

        // Redis Hash에서 해당 menuId의 현재 수량 조회
        HashOperations<String, Object, Object> hashOps = redisTemplate.opsForHash();
        Object existing = hashOps.get(key, menuId);

        // 기존 수량이 있으면 누적, 없으면 0부터 시작
        int currentQty = existing != null ? Integer.parseInt(existing.toString()) : 0;

        // 새 수량으로 덮어씀 (누적된 값)
        hashOps.put(key, menuId, String.valueOf(currentQty + request.getQuantity()));

        // TTL 30분 리셋 (담을 때마다 갱신)
        redisTemplate.expire(key, CART_TTL_MINUTES, TimeUnit.MINUTES);
    }

    // 장바구니 전체 조회
    public CartResponseDto getCart(String kioskUuid) {
        String key = CART_KEY_PREFIX + kioskUuid;

        // Redis Hash 전체 조회
        Map<Object, Object> cartItems = redisTemplate.opsForHash().entries(key);

        // 장바구니가 비어있으면 빈 응답 반환
        if (cartItems.isEmpty()) {
            return new CartResponseDto(kioskUuid, Collections.emptyList(), 0);
        }

        // Redis의 key(menuId 문자열)를 Long으로 변환
        List<Long> menuIds = cartItems.keySet().stream()
                .map(k -> Long.parseLong(k.toString()))
                .collect(Collectors.toList());

        // menuId 목록으로 DB에서 메뉴 정보 한 번에 조회 (N+1 방지)
        Map<Long, Menu> menuMap = menuRepository.findAllById(menuIds).stream()
                .collect(Collectors.toMap(Menu::getId, m -> m));

        // Redis 데이터 + DB 메뉴 정보 합쳐서 응답 DTO 생성
        List<CartItemResponseDto> items = cartItems.entrySet().stream()
                .map(e -> {
                    Long menuId = Long.parseLong(e.getKey().toString());
                    int qty = Integer.parseInt(e.getValue().toString());
                    Menu menu = menuMap.get(menuId);
                    return new CartItemResponseDto(menuId, menu.getName(), menu.getPrice(), qty, menu.getPrice() * qty);
                })
                .collect(Collectors.toList());

        // 전체 합계 계산
        int totalAmount = items.stream().mapToInt(CartItemResponseDto::subtotal).sum();
        return new CartResponseDto(kioskUuid, items, totalAmount);
    }

    // 장바구니에서 특정 메뉴 제거
    public void removeItem(String kioskUuid, Long menuId) {
        String key = CART_KEY_PREFIX + kioskUuid;
        String field = String.valueOf(menuId);

        // 해당 menuId가 장바구니에 있는지 확인
        Boolean exists = redisTemplate.opsForHash().hasKey(key, field);
        if (!Boolean.TRUE.equals(exists)) {
            throw new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND);
        }
        // Redis Hash에서 해당 field(menuId) 삭제
        redisTemplate.opsForHash().delete(key, field);
    }

    public void clearCart(String kioskUuid) {
        redisTemplate.delete(CART_KEY_PREFIX + kioskUuid);
    }

    // 장바구니 전체 결제 -> 중간에 하나라도 실패(품절, 포인트 부족 등)하면 전체 롤백
    @Transactional
    public CartCheckoutResponseDto checkout(String kioskUuid, CartCheckoutRequestDto request) {
        String key = CART_KEY_PREFIX + kioskUuid;

        // 장바구니 전체 조회
        Map<Object, Object> cartItems = redisTemplate.opsForHash().entries(key);
        if (cartItems.isEmpty()) {
            throw new BusinessException(ErrorCode.CART_EMPTY);
        }

        List<Long> orderIds = new ArrayList<>();
        int totalAmount = 0;
        long remainingPoint = 0;

        // 아이템마다 개별 주문 처리
        for (Map.Entry<Object, Object> entry : cartItems.entrySet()) {
            Long menuId = Long.parseLong(entry.getKey().toString());
            int quantity = Integer.parseInt(entry.getValue().toString());

            // 기존 OrderService 재사용 — quantity 포함해서 주문
            OrderCreateRequestDto orderRequest = new OrderCreateRequestDto(
                    request.getPhoneNumber(), menuId, request.getStoreId(), request.getKioskId(), quantity
            );
            OrderCreateResponseDto response = orderService.order(orderRequest);
            orderIds.add(response.orderId());
            totalAmount += response.totalPrice();
            remainingPoint = response.remainingPoint();   // 마지막 주문 후 잔여 포인트
        }

        // 모든 주문 성공 후 장바구니 비우기
        redisTemplate.delete(key);
        return new CartCheckoutResponseDto(orderIds, totalAmount, remainingPoint);
    }
}