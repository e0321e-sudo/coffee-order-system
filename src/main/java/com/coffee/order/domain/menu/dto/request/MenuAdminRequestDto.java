package com.coffee.order.domain.menu.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MenuAdminRequestDto {

    @NotNull
    private Long categoryId;

    @NotBlank
    private String name;

    @Min(0)
    private int price;
}