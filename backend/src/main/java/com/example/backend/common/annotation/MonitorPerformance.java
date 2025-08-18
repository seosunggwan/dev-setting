package com.example.backend.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 성능 모니터링을 위한 커스텀 어노테이션
 * 이 어노테이션이 붙은 메서드는 성능 모니터링 대상이 됩니다.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MonitorPerformance {
    
    /**
     * 성능 경고 임계값 (밀리초)
     * 기본값: 1000ms (1초)
     */
    long threshold() default 1000L;
    
    /**
     * 성능 모니터링 설명
     */
    String description() default "";
}
