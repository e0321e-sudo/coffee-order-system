package com.coffee.order.common.interceptor;

import com.coffee.order.common.exception.BusinessException;
import com.coffee.order.common.exception.ErrorCode;
import com.coffee.order.domain.kiosk.entity.Kiosk;
import com.coffee.order.domain.kiosk.service.KioskAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class KioskAuthInterceptorTest {

    @InjectMocks
    private KioskAuthInterceptor interceptor;

    @Mock
    private KioskAuthService kioskAuthService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private static final String VALID_UUID = "test-uuid-001";
    private static final String VALID_SECRET = "secret123";
    private final Object handler = new Object();

    private Kiosk validKiosk;

    @BeforeEach
    void setUp() {
        validKiosk = Kiosk.builder()
                .storeId(1L)
                .kioskUuid(VALID_UUID)
                .secretKey(VALID_SECRET)
                .name("메인 키오스크")
                .isActive(true)
                .build();
    }

    // ===== 성공 케이스 =====

    @Test
    @DisplayName("키오스크 인증 성공 - true 반환")
    void 키오스크_인증_성공() {
        // given
        given(request.getHeader("X-Kiosk-UUID")).willReturn(VALID_UUID);
        given(request.getHeader("X-Kiosk-Secret")).willReturn(VALID_SECRET);
        given(kioskAuthService.authenticate(VALID_UUID, VALID_SECRET)).willReturn(validKiosk);

        // when
        boolean result = interceptor.preHandle(request, response, handler);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("키오스크 인증 성공 시 KioskAuthService.authenticate 1회 호출")
    void 키오스크_인증_성공시_서비스_호출_검증() {
        // given
        given(request.getHeader("X-Kiosk-UUID")).willReturn(VALID_UUID);
        given(request.getHeader("X-Kiosk-Secret")).willReturn(VALID_SECRET);
        given(kioskAuthService.authenticate(VALID_UUID, VALID_SECRET)).willReturn(validKiosk);

        // when
        interceptor.preHandle(request, response, handler);

        // then
        then(kioskAuthService).should(times(1)).authenticate(VALID_UUID, VALID_SECRET);
    }

    // ===== 실패 케이스 =====

    @Test
    @DisplayName("X-Kiosk-UUID 헤더 없을 시 INVALID_KIOSK 예외")
    void UUID_헤더_없음_예외() {
        // given
        given(request.getHeader("X-Kiosk-UUID")).willReturn(null);
        given(request.getHeader("X-Kiosk-Secret")).willReturn(VALID_SECRET);
        given(kioskAuthService.authenticate(null, VALID_SECRET))
                .willThrow(new BusinessException(ErrorCode.INVALID_KIOSK));

        // when & then
        assertThatThrownBy(() -> interceptor.preHandle(request, response, handler))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_KIOSK);
    }

    @Test
    @DisplayName("X-Kiosk-Secret 헤더 없을 시 INVALID_KIOSK 예외")
    void SecretKey_헤더_없음_예외() {
        // given
        given(request.getHeader("X-Kiosk-UUID")).willReturn(VALID_UUID);
        given(request.getHeader("X-Kiosk-Secret")).willReturn(null);
        given(kioskAuthService.authenticate(VALID_UUID, null))
                .willThrow(new BusinessException(ErrorCode.INVALID_KIOSK));

        // when & then
        assertThatThrownBy(() -> interceptor.preHandle(request, response, handler))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_KIOSK);
    }

    @Test
    @DisplayName("UUID, SecretKey 헤더 모두 없을 시 INVALID_KIOSK 예외")
    void UUID_SecretKey_모두없음_예외() {
        // given
        given(request.getHeader("X-Kiosk-UUID")).willReturn(null);
        given(request.getHeader("X-Kiosk-Secret")).willReturn(null);
        given(kioskAuthService.authenticate(null, null))
                .willThrow(new BusinessException(ErrorCode.INVALID_KIOSK));

        // when & then
        assertThatThrownBy(() -> interceptor.preHandle(request, response, handler))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_KIOSK);
    }

    @Test
    @DisplayName("존재하지 않는 키오스크 UUID - INVALID_KIOSK 예외")
    void 존재하지않는_키오스크_UUID_예외() {
        // given
        given(request.getHeader("X-Kiosk-UUID")).willReturn("unknown-uuid");
        given(request.getHeader("X-Kiosk-Secret")).willReturn(VALID_SECRET);
        given(kioskAuthService.authenticate("unknown-uuid", VALID_SECRET))
                .willThrow(new BusinessException(ErrorCode.INVALID_KIOSK));

        // when & then
        assertThatThrownBy(() -> interceptor.preHandle(request, response, handler))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_KIOSK);
    }

    @Test
    @DisplayName("잘못된 SecretKey - INVALID_KIOSK 예외")
    void 잘못된_SecretKey_예외() {
        // given
        given(request.getHeader("X-Kiosk-UUID")).willReturn(VALID_UUID);
        given(request.getHeader("X-Kiosk-Secret")).willReturn("wrongSecret");
        given(kioskAuthService.authenticate(VALID_UUID, "wrongSecret"))
                .willThrow(new BusinessException(ErrorCode.INVALID_KIOSK));

        // when & then
        assertThatThrownBy(() -> interceptor.preHandle(request, response, handler))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_KIOSK);
    }

    @Test
    @DisplayName("UUID, SecretKey 모두 잘못된 경우 - INVALID_KIOSK 예외")
    void UUID_SecretKey_모두_잘못된경우_예외() {
        // given
        given(request.getHeader("X-Kiosk-UUID")).willReturn("wrong-uuid");
        given(request.getHeader("X-Kiosk-Secret")).willReturn("wrong-secret");
        given(kioskAuthService.authenticate("wrong-uuid", "wrong-secret"))
                .willThrow(new BusinessException(ErrorCode.INVALID_KIOSK));

        // when & then
        assertThatThrownBy(() -> interceptor.preHandle(request, response, handler))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_KIOSK);
    }
}