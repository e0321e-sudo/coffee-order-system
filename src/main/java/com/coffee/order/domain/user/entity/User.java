package com.coffee.order.domain.user.entity;

import com.coffee.order.common.exception.BusinessException;
import com.coffee.order.common.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 회원 엔티티 — 포인트 잔액 관리 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "phone_number", nullable = false, unique = true)
    private String phoneNumber;

    @Column(name = "point", nullable = false)
    private long point;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public User(String phoneNumber, long point) {
        this.phoneNumber = phoneNumber;
        this.point = point;
        this.createdAt = LocalDateTime.now();
    }

    /** 포인트 충전 */
    public void chargePoint(long amount) {
        this.point += amount;
    }

    /** 포인트 차감 — 잔액 부족 시 INSUFFICIENT_POINT 예외 */
    public void deductPoint(long amount) {
        if (this.point < amount) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_POINT);
        }
        this.point -= amount;
    }
}