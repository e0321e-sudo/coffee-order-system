package com.coffee.order.domain.user.controller;

import com.coffee.order.common.response.ApiResponse;
import com.coffee.order.domain.user.dto.request.PointChargeRequestDto;
import com.coffee.order.domain.user.dto.response.PointChargeResponseDto;
import com.coffee.order.domain.user.service.PointService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/points")
@RequiredArgsConstructor
public class PointController {

    private final PointService pointService;

    @PostMapping("/charge")
    public ApiResponse<PointChargeResponseDto> charge(@Valid @RequestBody PointChargeRequestDto request) {
        return ApiResponse.success(pointService.charge(request));
    }
}