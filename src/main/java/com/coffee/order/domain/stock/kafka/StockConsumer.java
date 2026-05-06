package com.coffee.order.domain.stock.kafka;

import com.coffee.order.domain.stock.kafka.event.StockAlertEvent;
import com.coffee.order.domain.stock.kafka.event.StockRestockedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StockConsumer {

    // stock-alert 토픽 구독 - 재고 부족 감지 시 수신
    @KafkaListener(
            topics = "stock-alert",
            groupId = "coffee-order-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeStockAlert(StockAlertEvent event) {
        log.info("재고 부족 이벤트 수신 - storeId: {}, menuId: {}, manuName: {}, currentStock: {}",
                event.getStoreId(), event.getMenuId(), event.getMenuName(), event.getCurrentStock());
    }

    //stock-restocked 토픽 구독 - 재입고 완료 시 수신
    @KafkaListener(
            topics = "stock-restocked",
            groupId = "coffee-order-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeStockRestocked(StockRestockedEvent event) {
        log.info("재입고 이벤트 수신 - storeId: {}, menuId: {}, manuName: {}, currentStock: {}",
                event.getStoreId(), event.getMenuId(), event.getMenuName(), event.getCurrentStock());
    }
}
