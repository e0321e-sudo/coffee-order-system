package com.coffee.order.domain.menu.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 메뉴 엔티티 — 카테고리별 메뉴 정보 및 표시 여부 관리 */
@Entity
@Table(name = "menus")
@Getter
@NoArgsConstructor
public class Menu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "price", nullable = false)
    private int price;

    @Column(name = "is_visible", nullable = false)
    private boolean isVisible;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public Menu(Long categoryId, String name, int price, boolean isVisible) {
        this.categoryId = categoryId;
        this.name = name;
        this.price = price;
        this.isVisible = isVisible;
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    private void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void update(Long categoryId, String name, int price) {
        this.categoryId = categoryId;
        this.name = name;
        this.price = price;
    }

    public void hide() {
        this.isVisible = false;
    }
}