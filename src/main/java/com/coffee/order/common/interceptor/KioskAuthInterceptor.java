package com.coffee.order.common.interceptor;

import com.coffee.order.domain.kiosk.service.KioskAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class KioskAuthInterceptor implements HandlerInterceptor {

    private final KioskAuthService kioskAuthService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String kioskUuid = request.getHeader("X-Kiosk-UUID");
        String secretKey = request.getHeader("X-Kiosk-Secret");
        kioskAuthService.authenticate(kioskUuid, secretKey);
        return true;
    }
}