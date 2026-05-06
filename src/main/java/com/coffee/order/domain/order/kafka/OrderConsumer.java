package com.coffee.order.domain.order.kafka;

import com.coffee.order.domain.order.dto.event.OrderCompletedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderConsumer {

    // order-completed 토픽 구독
    // groupId - 같은 그룹끼리 메세지를 나눠서 처리
    // containerFactory - KafkaConfig에서 만든 JSON 역직렬화 팩토리
    @KafkaListener(
            topics = "order-completed",
            groupId = "coffee-order-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(OrderCompletedEvent event) {
        // 1. 이벤트 수신 로그
        log.info("주문 완료 이벤트 수신 - orderId: {}, userId: {}, menuId: {}, totalPrice: {}",
                event.getOrderId(),
                event.getUserId(),
                event.getMenuId(),
                event.getTotalPrice()
                );

        // 2. Mock API - 실제 데이터 수집 플랫폼 대신 로그로 전송 시뮬레이션
        log.info("[데이터 수집 플랫폼 전송] phoneNumber: {}, menuId: {}, totalPrice: {}",
                event.getPhoneNumber(),
                event.getMenuId(),
                event.getTotalPrice()
                );
    }
}
