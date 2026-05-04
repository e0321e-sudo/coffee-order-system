package com.coffee.order.domain.user.dto.response;

public record PointChargeResponseDto(String phoneNumber, long chargedAmount, long totalPoint) {
}