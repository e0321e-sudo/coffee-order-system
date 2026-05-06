package com.coffee.order.domain.admin.controller;

import com.coffee.order.common.response.ApiResponse;
import com.coffee.order.domain.admin.dto.request.AdminLoginRequestDto;
import com.coffee.order.domain.admin.dto.request.TokenRefreshRequestDto;
import com.coffee.order.domain.admin.dto.response.AdminLoginResponseDto;
import com.coffee.order.domain.admin.dto.response.TokenRefreshResponseDto;
import com.coffee.order.domain.admin.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminService adminService;

    @PostMapping("/login")
    public ApiResponse<AdminLoginResponseDto> login(@Valid @RequestBody AdminLoginRequestDto request) {
        return ApiResponse.success(adminService.login(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenRefreshResponseDto> refresh(@Valid @RequestBody TokenRefreshRequestDto request) {
        return ApiResponse.success(adminService.refresh(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody TokenRefreshRequestDto request) {
        adminService.logout(request);
        return ApiResponse.success(null);
    }
}