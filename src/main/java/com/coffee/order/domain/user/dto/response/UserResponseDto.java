package com.coffee.order.domain.user.dto.response;

import com.coffee.order.domain.user.entity.User;

public record UserResponseDto(Long userId, String phoneNumber, long point, boolean isNewUser) {

    public static UserResponseDto from(User user, boolean isNewUser) {
        return new UserResponseDto(user.getId(), user.getPhoneNumber(), user.getPoint(), isNewUser);
    }
}