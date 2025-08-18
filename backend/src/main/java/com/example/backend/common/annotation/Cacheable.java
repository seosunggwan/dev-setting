package com.example.backend.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 메서드 결과를 캐시하기 위한 어노테이션
 * 이 어노테이션이 붙은 메서드의 결과는 Redis에 캐시됩니다.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Cacheable {
    
    /**
     * 캐시 만료 시간 (분)
     * 기본값: 10분
     */
    int expireMinutes() default 10;
}
