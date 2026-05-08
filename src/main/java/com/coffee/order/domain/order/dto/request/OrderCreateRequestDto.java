package com.coffee.order.domain.order.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateRequestDto {

    @NotBlank
    @Pattern(regexp = "^01[0-9]-\\d{3,4}-\\d{4}$", message = "전화번호 형식이 올바르지 않습니다. (예: 010-1234-5678)")
    private String phoneNumber;

    @NotNull
    private Long menuId;

    @NotNull
    private Long storeId;

    @NotNull
    private Long kioskId;

    @Min(1)
    private int quantity = 1;

    // 중복 요청 방지 키 (선택값 - 없으면 검사 안 함)
    private String idempotencyKey;
}