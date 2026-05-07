package com.coffee.order.domain.cart.controller;

import com.coffee.order.common.response.ApiResponse;
import com.coffee.order.domain.cart.dto.request.CartCheckoutRequestDto;
import com.coffee.order.domain.cart.dto.request.CartItemRequestDto;
import com.coffee.order.domain.cart.dto.response.CartCheckoutResponseDto;
import com.coffee.order.domain.cart.dto.response.CartResponseDto;
import com.coffee.order.domain.cart.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping("/items")
    public ApiResponse<Void> addItem(
            @RequestHeader("X-Kiosk-UUID") String kioskUuid,
            @Valid @RequestBody CartItemRequestDto request) {
        cartService.addItem(kioskUuid, request);
        return ApiResponse.success(null);
    }

    @GetMapping
    public ApiResponse<CartResponseDto> getCart(
            @RequestHeader("X-Kiosk-UUID") String kioskUuid) {
        return ApiResponse.success(cartService.getCart(kioskUuid));
    }

    @DeleteMapping("/items/{menuId}")
    public ApiResponse<Void> removeItem(
            @RequestHeader("X-Kiosk-UUID") String kioskUuid,
            @PathVariable Long menuId) {
        cartService.removeItem(kioskUuid, menuId);
        return ApiResponse.success(null);
    }

    @DeleteMapping
    public ApiResponse<Void> clearCart(
            @RequestHeader("X-Kiosk-UUID") String kioskUuid) {
        cartService.clearCart(kioskUuid);
        return ApiResponse.success(null);
    }

    @PostMapping("/checkout")
    public ApiResponse<CartCheckoutResponseDto> checkout(
            @RequestHeader("X-Kiosk-UUID") String kioskUuid,
            @Valid @RequestBody CartCheckoutRequestDto request) {
        return ApiResponse.success(cartService.checkout(kioskUuid, request));
    }
}