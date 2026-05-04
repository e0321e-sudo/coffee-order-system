package com.coffee.order.domain.admin.service;

import com.coffee.order.common.config.JwtUtil;
import com.coffee.order.common.exception.BusinessException;
import com.coffee.order.common.exception.ErrorCode;
import com.coffee.order.domain.admin.dto.request.AdminLoginRequestDto;
import com.coffee.order.domain.admin.dto.response.AdminLoginResponseDto;
import com.coffee.order.domain.admin.entity.Admin;
import com.coffee.order.domain.admin.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional(readOnly = true)
    public AdminLoginResponseDto login(AdminLoginRequestDto request) {
        Admin admin = adminRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_NOT_FOUND));
        if (!passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }
        return new AdminLoginResponseDto(jwtUtil.generateToken(admin.getEmail()));
    }
}