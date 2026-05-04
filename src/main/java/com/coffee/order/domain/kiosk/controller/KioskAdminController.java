package com.coffee.order.domain.kiosk.controller;

import com.coffee.order.common.response.ApiResponse;
import com.coffee.order.domain.kiosk.dto.request.KioskRequestDto;
import com.coffee.order.domain.kiosk.dto.response.KioskResponseDto;
import com.coffee.order.domain.kiosk.service.KioskAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/stores/{storeId}/kiosks")
@RequiredArgsConstructor
public class KioskAdminController {

    private final KioskAdminService kioskAdminService;

    @PostMapping
    public ApiResponse<KioskResponseDto> create(@PathVariable Long storeId,
                                                 @Valid @RequestBody KioskRequestDto request) {
        return ApiResponse.success(kioskAdminService.create(storeId, request));
    }

    @GetMapping
    public ApiResponse<List<KioskResponseDto>> findAll(@PathVariable Long storeId) {
        return ApiResponse.success(kioskAdminService.findByStoreId(storeId));
    }

    @DeleteMapping("/{kioskId}")
    public ApiResponse<Void> delete(@PathVariable Long storeId,
                                     @PathVariable Long kioskId) {
        kioskAdminService.delete(storeId, kioskId);
        return ApiResponse.success(null);
    }
}