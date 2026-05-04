package com.coffee.order.common.config;

import com.coffee.order.common.interceptor.KioskAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final KioskAuthInterceptor kioskAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(kioskAuthInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/admin/**");
    }
}