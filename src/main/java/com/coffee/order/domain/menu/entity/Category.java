package com.coffee.order.domain.menu.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 메뉴 카테고리 엔티티 — 노출 순서 및 표시 여부 관리 */
@Entity
@Table(name = "categories")
@Getter
@NoArgsConstructor
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "is_visible", nullable = false)
    private boolean isVisible;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Category(String name, int displayOrder, boolean isVisible) {
        this.name = name;
        this.displayOrder = displayOrder;
        this.isVisible = isVisible;
        this.createdAt = LocalDateTime.now();
    }

    public void update(String name, int displayOrder) {
        this.name = name;
        this.displayOrder = displayOrder;
    }

    public void hide() {
        this.isVisible = false;
    }
}