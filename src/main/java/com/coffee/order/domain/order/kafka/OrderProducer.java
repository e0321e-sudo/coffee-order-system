package com.coffee.order.domain.order.kafka;

import com.coffee.order.domain.order.dto.event.OrderCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderProducer {

    // KafkaConfig에서 설정한 KafkaTemplate<String, Object> 불러오기
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // 주문 완료 시 order-completed 토픽에 이벤트 발행
    public void sendOrderCompleted(OrderCompletedEvent event) {
        kafkaTemplate.send("order-completed", event);
        log.info("주문 완료 이벤트 발행 - orderId: {}", event.getOrderId());
    }
}
