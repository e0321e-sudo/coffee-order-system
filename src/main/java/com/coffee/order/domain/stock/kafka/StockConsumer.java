package com.coffee.order.domain.stock.kafka;

import com.coffee.order.common.sse.SseEmitterManager;
import com.coffee.order.domain.stock.kafka.event.StockAlertEvent;
import com.coffee.order.domain.stock.kafka.event.StockRestockedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockConsumer {

    private final SseEmitterManager sseEmitterManager;

    // stock-alert 토픽 구독 - 재고 부족 감지 시 수신
    @KafkaListener(
            topics = "stock-alert",
            groupId = "coffee-order-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeStockAlert(StockAlertEvent event) {
        log.info("재고 부족 이벤트 수신 - storeId: {}, menuId: {}, menuName: {}, currentStock: {}",
                event.getStoreId(), event.getMenuId(), event.getMenuName(), event.getCurrentStock());

        // SSE로 모든 관리자에게 재고 부족 알림 전송
        sseEmitterManager.sendToAll("stock-alert",
                "[" + event.getStoreName() + "] " + "재고 부족 알림 - " + event.getMenuName() +
                        " (잔여 " + event.getCurrentStock() + "개)");
    }

    //stock-restocked 토픽 구독 - 재입고 완료 시 수신
    @KafkaListener(
            topics = "stock-restocked",
            groupId = "coffee-order-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeStockRestocked(StockRestockedEvent event) {
        log.info("재입고 이벤트 수신 - storeId: {}, menuId: {}, menuName: {}, currentStock: {}",
                event.getStoreId(), event.getMenuId(), event.getMenuName(), event.getCurrentStock());

        // SSE로 모든 관리자에게 재입고 알림 전송
        sseEmitterManager.sendToAll("stock-restocked",
                "[" + event.getStoreName() + "] " + "재입고 완료 알림 - " + event.getMenuName() +
                        " (+" + event.getRestockAmount() + "개, 현재 " + event.getCurrentStock() + "개)");
    }
}
