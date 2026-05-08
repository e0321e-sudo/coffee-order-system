package com.coffee.order.common.service;

import com.coffee.order.common.exception.BusinessException;
import com.coffee.order.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @InjectMocks
    private IdempotencyService idempotencyService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOps;

    @BeforeEach
    void setUp() {
        given(redisTemplate.opsForValue()).willReturn(valueOps);
    }

    // ===== checkAndSave - 성공 케이스 =====

    @Test
    @DisplayName("최초 요청 - Redis에 키 저장 후 정상 처리")
    void 최초_요청_정상_처리() {
        // given
        String key = "idempotency:order:uuid-1234";
        given(valueOps.setIfAbsent(key, "processed", 5L, TimeUnit.MINUTES)).willReturn(true);

        // when & then: 예외 없이 통과
        assertThatCode(() -> idempotencyService.checkAndSave("uuid-1234", "order"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("최초 요청 - Redis setIfAbsent 호출 검증")
    void 최초_요청_setIfAbsent_호출_검증() {
        // given
        String key = "idempotency:order:uuid-1234";
        given(valueOps.setIfAbsent(key, "processed", 5L, TimeUnit.MINUTES)).willReturn(true);

        // when
        idempotencyService.checkAndSave("uuid-1234", "order");

        // then
        then(valueOps).should(times(1)).setIfAbsent(key, "processed", 5L, TimeUnit.MINUTES);
    }

    @Test
    @DisplayName("최초 요청 - TTL 5분으로 저장")
    void 최초_요청_TTL_5분_저장() {
        // given
        given(valueOps.setIfAbsent(anyString(), eq("processed"), eq(5L), eq(TimeUnit.MINUTES)))
                .willReturn(true);

        // when
        idempotencyService.checkAndSave("uuid-abc", "order");

        // then: TTL 5분 검증
        then(valueOps).should().setIfAbsent(anyString(), eq("processed"), eq(5L), eq(TimeUnit.MINUTES));
    }

    // ===== checkAndSave - 실패 케이스 =====

    @Test
    @DisplayName("중복 요청 - DUPLICATE_REQUEST 예외 발생")
    void 중복_요청_DUPLICATE_REQUEST_예외() {
        // given: Redis에 키가 이미 존재 (setIfAbsent = false)
        String key = "idempotency:order:uuid-1234";
        given(valueOps.setIfAbsent(key, "processed", 5L, TimeUnit.MINUTES)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> idempotencyService.checkAndSave("uuid-1234", "order"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DUPLICATE_REQUEST);
    }

    @Test
    @DisplayName("setIfAbsent null 반환 - 중복으로 간주하여 DUPLICATE_REQUEST 예외")
    void setIfAbsent_null_반환_중복_예외() {
        // given: Redis 연결 불안정 등으로 null 반환
        given(valueOps.setIfAbsent(anyString(), any(), anyLong(), any())).willReturn(null);

        // when & then: null은 Boolean.TRUE.equals 에서 false → 중복으로 간주
        assertThatThrownBy(() -> idempotencyService.checkAndSave("uuid-null", "order"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DUPLICATE_REQUEST);
    }

    @Test
    @DisplayName("type이 다르면 같은 키라도 별개 키로 저장")
    void 다른_type_같은_키_별개_처리() {
        // given: type "order" 와 "cancel" 은 Redis 키가 다름
        given(valueOps.setIfAbsent("idempotency:order:uuid-same", "processed", 5L, TimeUnit.MINUTES))
                .willReturn(true);
        given(valueOps.setIfAbsent("idempotency:cancel:uuid-same", "processed", 5L, TimeUnit.MINUTES))
                .willReturn(true);

        // when & then: 둘 다 예외 없이 통과
        assertThatCode(() -> idempotencyService.checkAndSave("uuid-same", "order"))
                .doesNotThrowAnyException();
        assertThatCode(() -> idempotencyService.checkAndSave("uuid-same", "cancel"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Redis 키 형식 검증 - idempotency:{type}:{key}")
    void Redis_키_형식_검증() {
        // given
        given(valueOps.setIfAbsent(anyString(), any(), anyLong(), any())).willReturn(true);

        // when
        idempotencyService.checkAndSave("my-key-123", "order");

        // then: "idempotency:order:my-key-123" 형식으로 저장
        then(valueOps).should().setIfAbsent(
                eq("idempotency:order:my-key-123"),
                eq("processed"),
                eq(5L),
                eq(TimeUnit.MINUTES)
        );
    }
}