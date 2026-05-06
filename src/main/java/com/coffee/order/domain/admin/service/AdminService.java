package com.coffee.order.domain.admin.service;

import com.coffee.order.common.config.JwtUtil;
import com.coffee.order.common.exception.BusinessException;
import com.coffee.order.common.exception.ErrorCode;
import com.coffee.order.domain.admin.dto.request.AdminLoginRequestDto;
import com.coffee.order.domain.admin.dto.request.TokenRefreshRequestDto;
import com.coffee.order.domain.admin.dto.response.AdminLoginResponseDto;
import com.coffee.order.domain.admin.dto.response.TokenRefreshResponseDto;
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

    @Transactional
    public AdminLoginResponseDto login(AdminLoginRequestDto request) {
        Admin admin = adminRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_NOT_FOUND));
        if (!passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }
        String accessToken = jwtUtil.generateToken(admin.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken(admin.getEmail());
        admin.updateRefreshToken(refreshToken);
        return new AdminLoginResponseDto(accessToken, refreshToken);
    }

    @Transactional
    public TokenRefreshResponseDto refresh(TokenRefreshRequestDto request) {
        String refreshToken = request.getRefreshToken();
        if (!jwtUtil.isValid(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
        String email = jwtUtil.extractEmail(refreshToken);
        Admin admin = adminRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_NOT_FOUND));
        if (!refreshToken.equals(admin.getRefreshToken())) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
        return new TokenRefreshResponseDto(jwtUtil.generateToken(admin.getEmail()));
    }

    @Transactional
    public void logout(TokenRefreshRequestDto request) {
        String refreshToken = request.getRefreshToken();
        if (!jwtUtil.isValid(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
        String email = jwtUtil.extractEmail(refreshToken);
        Admin admin = adminRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_NOT_FOUND));
        admin.updateRefreshToken(null);
    }
}