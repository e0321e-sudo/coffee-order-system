package com.coffee.order.domain.kiosk.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 키오스크 엔티티 — 매장별 키오스크 장치 정보 */
@Entity
@Table(name = "kiosks")
@Getter
@NoArgsConstructor
public class Kiosk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "kiosk_uuid", nullable = false, unique = true)
    private String kioskUuid;

    @Column(name = "secret_key", nullable = false)
    private String secretKey;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Kiosk(Long storeId, String kioskUuid, String secretKey, String name, boolean isActive) {
        this.storeId = storeId;
        this.kioskUuid = kioskUuid;
        this.secretKey = secretKey;
        this.name = name;
        this.isActive = isActive;
        this.createdAt = LocalDateTime.now();
    }
}