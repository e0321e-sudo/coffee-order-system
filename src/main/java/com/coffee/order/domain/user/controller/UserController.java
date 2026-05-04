package com.coffee.order.domain.user.controller;

import com.coffee.order.common.response.ApiResponse;
import com.coffee.order.domain.user.dto.response.UserResponseDto;
import com.coffee.order.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{phoneNumber}")
    public ApiResponse<UserResponseDto> getUser(@PathVariable String phoneNumber) {
        return ApiResponse.success(userService.getUser(phoneNumber));
    }
}