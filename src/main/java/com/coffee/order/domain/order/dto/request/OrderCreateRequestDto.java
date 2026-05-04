package com.coffee.order.domain.order.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateRequestDto {

    @NotBlank
    private String phoneNumber;

    @NotNull
    private Long menuId;

    @NotNull
    private Long storeId;

    @NotNull
    private Long kioskId;
}