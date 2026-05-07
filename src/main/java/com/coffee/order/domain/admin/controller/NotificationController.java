package com.coffee.order.domain.admin.controller;

import com.coffee.order.common.sse.SseEmitterManager;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final SseEmitterManager sseEmitterManager;

    // 백오피스 관리자 SSE 구독
    // JWT 인증된 관리자만 접근 가능
    @GetMapping("/stream")
    public SseEmitter stream(Authentication authentication) {
        // JWT에서 관리자 email 추출
        String email = authentication.getName();
        return sseEmitterManager.add(email);
    }
}
