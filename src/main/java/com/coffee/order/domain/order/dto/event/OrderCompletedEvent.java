package com.coffee.order.domain.order.dto.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderCompletedEvent {
    private Long orderId;
    private Long userId;
    private String phoneNumber; // 데이터 수집 플랫폼 전송용
    private Long menuId;
    private Long storeId;
    private Long kioskId;
    private int totalPrice;
    private LocalDateTime createdAt;
}
