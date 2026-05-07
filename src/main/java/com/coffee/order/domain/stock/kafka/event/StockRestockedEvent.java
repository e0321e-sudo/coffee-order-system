package com.coffee.order.domain.stock.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class StockRestockedEvent {
    private Long storeId;       // 어느 매장
    private String storeName;   // 매장 이름
    private Long menuId;        // 어느 메뉴
    private String menuName;    // 메뉴명
    private int currentStock;   // 재입고 후 재고
    private int restockAmount;  // 재입고 수량
}
