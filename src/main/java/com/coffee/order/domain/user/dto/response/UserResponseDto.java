package com.coffee.order.domain.user.dto.response;

import com.coffee.order.domain.user.entity.User;

public record UserResponseDto(Long userId, String phoneNumber, long point) {

    public static UserResponseDto from(User user) {
        return new UserResponseDto(user.getId(), user.getPhoneNumber(), user.getPoint());
    }
}