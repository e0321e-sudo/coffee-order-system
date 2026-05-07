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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @InjectMocks
    private AdminService adminService;

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    private Admin admin;

    @BeforeEach
    void setUp() {
        admin = Admin.builder()
                .email("admin@test.com")
                .password("encodedPassword")
                .build();
    }

    // ===== login =====

    @Test
    @DisplayName("관리자 로그인 성공 - accessToken + refreshToken 반환")
    void 로그인_성공() {
        // given
        AdminLoginRequestDto request = new AdminLoginRequestDto("admin@test.com", "1234");
        given(adminRepository.findByEmail("admin@test.com")).willReturn(Optional.of(admin));
        given(passwordEncoder.matches("1234", "encodedPassword")).willReturn(true);
        given(jwtUtil.generateToken("admin@test.com")).willReturn("accessToken");
        given(jwtUtil.generateRefreshToken("admin@test.com")).willReturn("refreshToken");

        // when
        AdminLoginResponseDto response = adminService.login(request);

        // then
        assertThat(response.accessToken()).isEqualTo("accessToken");
        assertThat(response.refreshToken()).isEqualTo("refreshToken");
    }

    @Test
    @DisplayName("로그인 성공 시 Admin 엔티티에 refreshToken 저장")
    void 로그인_성공시_refreshToken_저장() {
        // given
        AdminLoginRequestDto request = new AdminLoginRequestDto("admin@test.com", "1234");
        given(adminRepository.findByEmail("admin@test.com")).willReturn(Optional.of(admin));
        given(passwordEncoder.matches("1234", "encodedPassword")).willReturn(true);
        given(jwtUtil.generateToken("admin@test.com")).willReturn("accessToken");
        given(jwtUtil.generateRefreshToken("admin@test.com")).willReturn("savedRefreshToken");

        // when
        adminService.login(request);

        // then: Admin 엔티티에 refreshToken이 저장되었는지 확인
        assertThat(admin.getRefreshToken()).isEqualTo("savedRefreshToken");
    }

    @Test
    @DisplayName("로그인 - 존재하지 않는 이메일 ADMIN_NOT_FOUND 예외")
    void 로그인_이메일없음_예외() {
        // given
        AdminLoginRequestDto request = new AdminLoginRequestDto("ghost@test.com", "1234");
        given(adminRepository.findByEmail("ghost@test.com")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminService.login(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ADMIN_NOT_FOUND);
    }

    @Test
    @DisplayName("로그인 - 비밀번호 불일치 INVALID_PASSWORD 예외")
    void 로그인_비밀번호불일치_예외() {
        // given
        AdminLoginRequestDto request = new AdminLoginRequestDto("admin@test.com", "wrongPassword");
        given(adminRepository.findByEmail("admin@test.com")).willReturn(Optional.of(admin));
        given(passwordEncoder.matches("wrongPassword", "encodedPassword")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> adminService.login(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PASSWORD);
    }

    @Test
    @DisplayName("로그인 - 빈 비밀번호 입력 시 INVALID_PASSWORD 예외")
    void 로그인_빈비밀번호_예외() {
        // given
        AdminLoginRequestDto request = new AdminLoginRequestDto("admin@test.com", "");
        given(adminRepository.findByEmail("admin@test.com")).willReturn(Optional.of(admin));
        given(passwordEncoder.matches("", "encodedPassword")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> adminService.login(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PASSWORD);
    }

    // ===== refresh =====

    @Test
    @DisplayName("토큰 갱신 성공 - 새 accessToken 반환")
    void 토큰_갱신_성공() {
        // given
        String storedRefreshToken = "validRefreshToken";
        admin.updateRefreshToken(storedRefreshToken);
        TokenRefreshRequestDto request = new TokenRefreshRequestDto(storedRefreshToken);

        given(jwtUtil.isValid(storedRefreshToken)).willReturn(true);
        given(jwtUtil.extractEmail(storedRefreshToken)).willReturn("admin@test.com");
        given(adminRepository.findByEmail("admin@test.com")).willReturn(Optional.of(admin));
        given(jwtUtil.generateToken("admin@test.com")).willReturn("newAccessToken");

        // when
        TokenRefreshResponseDto response = adminService.refresh(request);

        // then
        assertThat(response.accessToken()).isEqualTo("newAccessToken");
    }

    @Test
    @DisplayName("토큰 갱신 - 유효하지 않은 토큰 INVALID_TOKEN 예외")
    void 토큰_갱신_유효하지않은토큰_예외() {
        // given
        TokenRefreshRequestDto request = new TokenRefreshRequestDto("invalidToken");
        given(jwtUtil.isValid("invalidToken")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> adminService.refresh(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_TOKEN);
    }

    @Test
    @DisplayName("토큰 갱신 - 저장된 토큰과 불일치 INVALID_TOKEN 예외")
    void 토큰_갱신_저장토큰_불일치_예외() {
        // given: DB에 저장된 refreshToken과 요청된 refreshToken이 다름
        admin.updateRefreshToken("storedToken");
        TokenRefreshRequestDto request = new TokenRefreshRequestDto("differentToken");

        given(jwtUtil.isValid("differentToken")).willReturn(true);
        given(jwtUtil.extractEmail("differentToken")).willReturn("admin@test.com");
        given(adminRepository.findByEmail("admin@test.com")).willReturn(Optional.of(admin));

        // when & then
        assertThatThrownBy(() -> adminService.refresh(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_TOKEN);
    }

    @Test
    @DisplayName("토큰 갱신 - refreshToken null 상태(로그아웃 후) INVALID_TOKEN 예외")
    void 토큰_갱신_로그아웃후_null토큰_예외() {
        // given: 로그아웃으로 인해 저장된 refreshToken이 null
        admin.updateRefreshToken(null);
        TokenRefreshRequestDto request = new TokenRefreshRequestDto("someToken");

        given(jwtUtil.isValid("someToken")).willReturn(true);
        given(jwtUtil.extractEmail("someToken")).willReturn("admin@test.com");
        given(adminRepository.findByEmail("admin@test.com")).willReturn(Optional.of(admin));

        // when & then
        assertThatThrownBy(() -> adminService.refresh(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_TOKEN);
    }

    @Test
    @DisplayName("토큰 갱신 - 관리자 없음 ADMIN_NOT_FOUND 예외")
    void 토큰_갱신_관리자없음_예외() {
        // given
        TokenRefreshRequestDto request = new TokenRefreshRequestDto("validToken");
        given(jwtUtil.isValid("validToken")).willReturn(true);
        given(jwtUtil.extractEmail("validToken")).willReturn("ghost@test.com");
        given(adminRepository.findByEmail("ghost@test.com")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminService.refresh(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ADMIN_NOT_FOUND);
    }

    // ===== logout =====

    @Test
    @DisplayName("로그아웃 성공 - Admin 엔티티 refreshToken null 처리")
    void 로그아웃_성공() {
        // given
        admin.updateRefreshToken("validRefreshToken");
        TokenRefreshRequestDto request = new TokenRefreshRequestDto("validRefreshToken");

        given(jwtUtil.isValid("validRefreshToken")).willReturn(true);
        given(jwtUtil.extractEmail("validRefreshToken")).willReturn("admin@test.com");
        given(adminRepository.findByEmail("admin@test.com")).willReturn(Optional.of(admin));

        // when
        adminService.logout(request);

        // then
        assertThat(admin.getRefreshToken()).isNull();
    }

    @Test
    @DisplayName("로그아웃 - 유효하지 않은 토큰 INVALID_TOKEN 예외")
    void 로그아웃_유효하지않은토큰_예외() {
        // given
        TokenRefreshRequestDto request = new TokenRefreshRequestDto("badToken");
        given(jwtUtil.isValid("badToken")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> adminService.logout(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_TOKEN);
    }

    @Test
    @DisplayName("로그아웃 - 관리자 없음 ADMIN_NOT_FOUND 예외")
    void 로그아웃_관리자없음_예외() {
        // given
        TokenRefreshRequestDto request = new TokenRefreshRequestDto("validToken");
        given(jwtUtil.isValid("validToken")).willReturn(true);
        given(jwtUtil.extractEmail("validToken")).willReturn("ghost@test.com");
        given(adminRepository.findByEmail("ghost@test.com")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminService.logout(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ADMIN_NOT_FOUND);
    }
}