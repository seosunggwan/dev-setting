package com.example.backend.common.aop;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;

@Slf4j
@Aspect
@Component
@ConditionalOnBean(RedisTemplate.class)  // Redis가 있을 때만 활성화
@RequiredArgsConstructor
public class CachingAspect {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 캐시 가능한 메서드에 대한 캐싱 처리
     */
    @Around("@annotation(com.example.backend.common.annotation.Cacheable)")
    public Object cacheResult(ProceedingJoinPoint joinPoint) throws Throwable {
        String cacheKey = generateCacheKey(joinPoint);
        
        // Redis에서 캐시 확인
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.info("📦 캐시 히트: {}", cacheKey);
            return cached;
        }
        
        // 캐시가 없으면 메서드 실행
        Object result = joinPoint.proceed();
        
        // 결과를 캐시에 저장 (10분)
        redisTemplate.opsForValue().set(cacheKey, result, Duration.ofMinutes(10));
        log.info("💾 캐시 저장: {}", cacheKey);
        
        return result;
    }

    /**
     * 캐시 무효화가 필요한 메서드에 대한 처리
     */
    @Around("@annotation(com.example.backend.common.annotation.CacheEvict)")
    public Object evictCache(ProceedingJoinPoint joinPoint) throws Throwable {
        String cacheKey = generateCacheKey(joinPoint);
        
        // 메서드 실행
        Object result = joinPoint.proceed();
        
        // 관련 캐시 삭제
        redisTemplate.delete(cacheKey);
        log.info("🗑️ 캐시 삭제: {}", cacheKey);
        
        return result;
    }

    private String generateCacheKey(ProceedingJoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String args = Arrays.toString(joinPoint.getArgs());
        
        return String.format("cache:%s:%s:%s", className, methodName, args.hashCode());
    }
}
