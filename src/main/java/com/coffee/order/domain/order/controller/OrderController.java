package com.coffee.order.domain.order.controller;

import com.coffee.order.common.response.ApiResponse;
import com.coffee.order.domain.order.dto.request.OrderCreateRequestDto;
import com.coffee.order.domain.order.dto.response.OrderCancelResponseDto;
import com.coffee.order.domain.order.dto.response.OrderCreateResponseDto;
import com.coffee.order.domain.order.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ApiResponse<OrderCreateResponseDto> order(@Valid @RequestBody OrderCreateRequestDto request) {
        return ApiResponse.success(orderService.order(request));
    }

    @PatchMapping("/{orderId}/cancel")
    public ApiResponse<OrderCancelResponseDto> cancel(
            @PathVariable Long orderId,
            @RequestParam
            @Pattern(regexp = "^01[0-9]-\\d{3,4}-\\d{4}$", message = "전화번호 형식이 올바르지 않습니다. (예: 010-1234-5678)")
            String phoneNumber) {
        return ApiResponse.success(orderService.cancel(orderId, phoneNumber));
    }
}