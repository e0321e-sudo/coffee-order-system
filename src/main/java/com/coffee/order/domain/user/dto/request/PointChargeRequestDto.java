package com.coffee.order.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PointChargeRequestDto {

    @NotBlank
    private String phoneNumber;

    @NotNull
    @Positive
    private Long amount;
}
