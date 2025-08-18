package com.example.backend.common.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class PerformanceAspect {

    /**
     * Repository 메서드 성능 모니터링
     */
    @Around("execution(* com.example.backend..repository.*Repository.*(..))")
    public Object monitorRepositoryPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        
        long startTime = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long executionTime = System.currentTimeMillis() - startTime;
        
        // 500ms 이상 걸리면 경고
        if (executionTime > 500) {
            log.warn("🐌 [{}] {} Repository 메서드 느림 ({}ms)", className, methodName, executionTime);
        }
        
        return result;
    }

    /**
     * 특정 메서드 성능 모니터링 (커스텀 어노테이션 사용)
     */
    @Around("@annotation(com.example.backend.common.annotation.MonitorPerformance)")
    public Object monitorPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        
        log.info("📊 [{}] {} 성능 모니터링 시작", className, methodName);
        
        long startTime = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long executionTime = System.currentTimeMillis() - startTime;
        
        log.info("📊 [{}] {} 성능 모니터링 완료 ({}ms)", className, methodName, executionTime);
        
        return result;
    }
}
