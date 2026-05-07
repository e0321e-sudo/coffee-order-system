package com.coffee.order.common.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class SseEmitterManager {

    // 연결된 관리자들을 저장 (email -> SseEmitter)
    // ConcurrentHashMap - 다중 서버 환경에서 동시 접근 안전
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    // 관리자 SSE 구독 등록
    public SseEmitter add(String email) {
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);  // 30분 타임아웃

        emitters.put(email, emitter);

        // 연결 완료/타임아웃/에러 시 자동 제거
        emitter.onCompletion(() -> emitters.remove(email));
        emitter.onTimeout(() -> emitters.remove(email));
        emitter.onError(e -> emitters.remove(email));

        return emitter;
    }

    // 모든 관리자에게 알림 전송
    public void sendToAll(String eventName, Object data) {
        emitters.forEach((email, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data));
            } catch (IOException e) {
                log.error("SSE 전송 실패 - email: {}", email);
                emitters.remove(email);
            }
        });
    }
}
