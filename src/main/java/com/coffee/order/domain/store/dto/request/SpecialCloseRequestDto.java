package com.coffee.order.domain.store.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SpecialCloseRequestDto {

    @NotNull
    private LocalDate closeDate;

    private String reason;
}