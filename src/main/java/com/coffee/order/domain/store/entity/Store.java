package com.coffee.order.domain.store.entity;

import com.coffee.order.common.exception.BusinessException;
import com.coffee.order.common.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** 매장 엔티티 — 영업 상태 및 특별 휴무 검증 */
@Entity
@Table(name = "stores")
@Getter
@NoArgsConstructor
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "address", nullable = false)
    private String address;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Store(String name, String address, boolean isActive) {
        this.name = name;
        this.address = address;
        this.isActive = isActive;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * 영업 가능 여부 검증 — 비활성 매장이거나 특별 휴무일이면 STORE_CLOSED 예외
     * SpecialClose 목록은 서비스 계층에서 조회 후 파라미터로 전달
     */
    public void validateOpen(LocalDate today, List<SpecialClose> specialCloses) {
        if (!this.isActive) {
            throw new BusinessException(ErrorCode.STORE_CLOSED);
        }
        boolean isSpecialClosed = specialCloses.stream()
                .anyMatch(sc -> sc.getCloseDate().equals(today));
        if (isSpecialClosed) {
            throw new BusinessException(ErrorCode.STORE_CLOSED);
        }
    }

    public void update(String name, String address) {
        this.name = name;
        this.address = address;
    }
}