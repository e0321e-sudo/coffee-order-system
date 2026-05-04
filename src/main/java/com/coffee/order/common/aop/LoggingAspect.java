package com.coffee.order.common.aop;

import com.coffee.order.domain.admin.dto.request.AdminLoginRequestDto;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Pointcut("execution(* com.coffee.order.domain..service..*(..))")
    void serviceLayer() {}

    @Around("serviceLayer()")
    public Object logServiceMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String params = maskParams(joinPoint.getArgs());
        long start = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long elapsed = System.currentTimeMillis() - start;
            log.info("[SERVICE] {} | 파라미터: {} | 실행시간: {}ms | 결과: SUCCESS",
                    methodName, params, elapsed);
            return result;
        } catch (Throwable e) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("[SERVICE] {} | 파라미터: {} | 실행시간: {}ms | 결과: FAIL",
                    methodName, params, elapsed);
            throw e;
        }
    }

    private String maskParams(Object[] args) {
        return Arrays.stream(args)
                .map(arg -> {
                    if (arg instanceof AdminLoginRequestDto dto) {
                        return "AdminLoginRequestDto{email=" + dto.getEmail() + ", password=***}";
                    }
                    return String.valueOf(arg);
                })
                .collect(Collectors.joining(", ", "[", "]"));
    }
}