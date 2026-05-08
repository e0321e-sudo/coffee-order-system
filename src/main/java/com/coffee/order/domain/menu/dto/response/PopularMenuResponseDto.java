package com.coffee.order.domain.menu.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PopularMenuResponseDto {
    private Long menuId;
    private String name;
    private int price;
    private String categoryName;
    private Long orderCount;
}