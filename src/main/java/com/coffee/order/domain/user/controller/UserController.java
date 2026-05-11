package com.coffee.order.domain.user.controller;

import com.coffee.order.common.response.ApiResponse;
import com.coffee.order.domain.user.dto.response.UserResponseDto;
import com.coffee.order.domain.user.service.UserService;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{phoneNumber}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<UserResponseDto> getUser(
            @PathVariable
            @Pattern(regexp = "^01[0-9]-\\d{3,4}-\\d{4}$", message = "전화번호 형식이 올바르지 않습니다. (예: 010-1234-5678)")
            String phoneNumber) {
        return ApiResponse.success(userService.getUser(phoneNumber));
    }
}