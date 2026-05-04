package com.coffee.order.domain.store.controller;

import com.coffee.order.common.response.ApiResponse;
import com.coffee.order.domain.store.dto.request.SpecialCloseRequestDto;
import com.coffee.order.domain.store.dto.response.SpecialCloseResponseDto;
import com.coffee.order.domain.store.service.SpecialCloseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/stores/{storeId}/special-closes")
@RequiredArgsConstructor
public class SpecialCloseController {

    private final SpecialCloseService specialCloseService;

    @PostMapping
    public ApiResponse<SpecialCloseResponseDto> create(@PathVariable Long storeId,
                                                        @Valid @RequestBody SpecialCloseRequestDto request) {
        return ApiResponse.success(specialCloseService.create(storeId, request));
    }

    @GetMapping
    public ApiResponse<List<SpecialCloseResponseDto>> findAll(@PathVariable Long storeId) {
        return ApiResponse.success(specialCloseService.findByStoreId(storeId));
    }

    @DeleteMapping("/{specialCloseId}")
    public ApiResponse<Void> delete(@PathVariable Long storeId,
                                     @PathVariable Long specialCloseId) {
        specialCloseService.delete(storeId, specialCloseId);
        return ApiResponse.success(null);
    }
}