package com.coffee.order.domain.stock.kafka;

import com.coffee.order.domain.stock.kafka.event.StockAlertEvent;
import com.coffee.order.domain.stock.kafka.event.StockRestockedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    // 재고 10개 이하 감지 시 stock-alert 토픽에 이벤트 발행
    public void sendStockAlert(StockAlertEvent event) {
        kafkaTemplate.send("stock-alert", event);
        log.info("재고 부족 이벤트 발행 - storedId: {}, menuId: {}, currentStock: {}",
                event.getStoreId(),event.getMenuId(), event.getCurrentStock());
    }

    // 관리자 재입고 시 stock-restocked 토픽에 이벤트 발행
    public void sendStockRestocked(StockRestockedEvent event) {
        kafkaTemplate.send("stock-restocked", event);
        log.info("재입고 이벤트 발행 - storeId: {}, menuId: {}, currentStock: {}",
                event.getStoreId(), event.getMenuId(), event.getCurrentStock());
    }
}
