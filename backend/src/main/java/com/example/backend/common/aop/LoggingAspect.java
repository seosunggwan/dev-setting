package com.example.backend.common.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class LoggingAspect {

    /**
     * 모든 Controller 메서드에 대한 로깅
     */
    @Around("@annotation(org.springframework.web.bind.annotation.RequestMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.GetMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PutMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.DeleteMapping)")
    public Object logControllerMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        
        log.info("🚀 [{}] {} 메서드 시작", className, methodName);
        
        long startTime = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;
            
            log.info("✅ [{}] {} 메서드 완료 ({}ms)", className, methodName, executionTime);
            return result;
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("❌ [{}] {} 메서드 실패 ({}ms) - {}", className, methodName, executionTime, e.getMessage());
            throw e;
        }
    }

    /**
     * Service 메서드에 대한 로깅
     */
    @Around("execution(* com.example.backend..service.*Service.*(..))")
    public Object logServiceMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        
        log.debug("🔧 [{}] {} 서비스 메서드 시작", className, methodName);
        
        long startTime = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;
            
            if (executionTime > 1000) { // 1초 이상 걸리면 경고
                log.warn("⚠️ [{}] {} 서비스 메서드 느림 ({}ms)", className, methodName, executionTime);
            } else {
                log.debug("✅ [{}] {} 서비스 메서드 완료 ({}ms)", className, methodName, executionTime);
            }
            return result;
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("❌ [{}] {} 서비스 메서드 실패 ({}ms) - {}", className, methodName, executionTime, e.getMessage());
            throw e;
        }
    }
}
