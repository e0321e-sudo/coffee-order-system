package com.coffee.order.domain.menu.entity;

import com.coffee.order.common.exception.BusinessException;
import com.coffee.order.common.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 매장별 메뉴 재고 엔티티 — 재고 증감 및 품절 상태 관리 */
@Entity
@Table(name = "menu_stocks")
@Getter
@NoArgsConstructor
public class MenuStock {

    public static final int LOW_STOCK_THRESHOLD = 10;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "menu_id", nullable = false)
    private Long menuId;

    @Column(name = "stock", nullable = false)
    private int stock;

    @Column(name = "is_sold_out", nullable = false)
    private boolean isSoldOut;

    @Builder
    public MenuStock(Long storeId, Long menuId, int stock) {
        this.storeId = storeId;
        this.menuId = menuId;
        this.stock = stock;
        this.isSoldOut = stock == 0;
    }

    /** 품절 여부 검증 — 품절이면 MENU_SOLD_OUT 예외 */
    public void validateNotSoldOut() {
        if (this.isSoldOut) {
            throw new BusinessException(ErrorCode.MENU_SOLD_OUT);
        }
    }

    /** 재고 1 차감 — 0이 되면 품절 처리 */
    public void decreaseStock() {
        validateNotSoldOut();
        this.stock--;
        if (this.stock == 0) {
            this.isSoldOut = true;
        }
    }

    /** 재고 1 증가 — 품절 상태 해제 */
    public void increaseStock() {
        this.stock++;
        this.isSoldOut = false;
    }

    public void toggleSoldOut() {
        this.isSoldOut = !this.isSoldOut;
    }

    public void addStock(int amount) {
        this.stock += amount;
        if (this.stock > 0) {
            this.isSoldOut = false;
        }
    }
}