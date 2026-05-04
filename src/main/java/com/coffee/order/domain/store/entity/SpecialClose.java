package com.coffee.order.domain.store.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 매장 특별 휴무일 엔티티 */
@Entity
@Table(name = "special_closes")
@Getter
@NoArgsConstructor
public class SpecialClose {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "close_date", nullable = false)
    private LocalDate closeDate;

    @Column(name = "reason")
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public SpecialClose(Long storeId, LocalDate closeDate, String reason) {
        this.storeId = storeId;
        this.closeDate = closeDate;
        this.reason = reason;
        this.createdAt = LocalDateTime.now();
    }
}