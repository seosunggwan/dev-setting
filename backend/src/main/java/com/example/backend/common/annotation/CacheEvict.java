package com.example.backend.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 캐시를 무효화하기 위한 어노테이션
 * 이 어노테이션이 붙은 메서드 실행 후 관련 캐시가 삭제됩니다.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheEvict {
}
