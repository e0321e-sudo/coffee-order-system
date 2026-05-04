package com.coffee.order.domain.order.controller;

import com.coffee.order.common.response.ApiResponse;
import com.coffee.order.domain.order.dto.request.OrderCreateRequestDto;
import com.coffee.order.domain.order.dto.response.OrderCancelResponseDto;
import com.coffee.order.domain.order.dto.response.OrderCreateResponseDto;
import com.coffee.order.domain.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
            @RequestParam String phoneNumber) {
        return ApiResponse.success(orderService.cancel(orderId, phoneNumber));
    }
}