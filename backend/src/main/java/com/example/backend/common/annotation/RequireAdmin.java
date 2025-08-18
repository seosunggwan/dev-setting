package com.example.backend.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 관리자 권한이 필요한 메서드에 사용하는 어노테이션
 * 이 어노테이션이 붙은 메서드는 ADMIN 권한을 가진 사용자만 접근 가능합니다.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireAdmin {
}
