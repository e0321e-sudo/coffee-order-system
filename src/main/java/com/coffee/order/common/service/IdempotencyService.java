package com.coffee.order.common.service;

import com.coffee.order.common.exception.BusinessException;
import com.coffee.order.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final RedisTemplate<String , Object> redisTemplate;

    // Redis 키 접두사 - 예: "idempotency:order:uuid-1234
    private static final String KEY_PREFIX = "idempotency:";

    // 중복 요청 방지 유효시간: 5분
    // 5분 안에 같은 키로 요청 오면 중복으로 간주
    private static final long TTL_MINUTES = 5;

    // 중복 요청 검사 (키 없으면 Redis에 저장 후 정상 처리, 키 있으면 예외 발생)
    public void checkAndSave(String idempotencyKey, String type) {
        String key = KEY_PREFIX + type + ":" + idempotencyKey;

        // Redis에 키가 이미 있는지 확인
        // setIfAbsent = 없을 때만 저장 (있으면 저장 안 함)
        Boolean isNew = redisTemplate.opsForValue()
                .setIfAbsent(key, "processed", TTL_MINUTES, TimeUnit.MINUTES);

        if (!Boolean.TRUE.equals(isNew)) {
            // 키가 이미 있음 -> 중복 요청
            log.info("[Idempotency] 중복 요청 감지 - key: {}", key);
            throw new BusinessException(ErrorCode.DUPLICATE_REQUEST);
        }
        log.info("[idempotency] 새 요청 등록 - key: {}", key);
    }
}
