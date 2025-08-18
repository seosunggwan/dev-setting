package com.example.backend.common.aop;

import com.example.backend.security.jwt.JWTUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class SecurityAspect {

    private final JWTUtil jwtUtil;

    /**
     * 관리자 권한이 필요한 메서드에 대한 검증
     */
    @Before("@annotation(com.example.backend.common.annotation.RequireAdmin)")
    public void checkAdminRole(JoinPoint joinPoint) {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            throw new SecurityException("요청 정보를 찾을 수 없습니다.");
        }

        String token = extractToken(request);
        if (token == null) {
            throw new SecurityException("토큰이 없습니다.");
        }

        String role = jwtUtil.getRole(token);
        if (!"ADMIN".equals(role)) {
            log.warn("🚫 관리자 권한 없음: {}", joinPoint.getSignature().getName());
            throw new SecurityException("관리자 권한이 필요합니다.");
        }

        log.info("🔒 관리자 권한 확인 완료: {}", joinPoint.getSignature().getName());
    }

    /**
     * 인증이 필요한 메서드에 대한 검증
     */
    @Before("@annotation(com.example.backend.common.annotation.RequireAuth)")
    public void checkAuthentication(JoinPoint joinPoint) {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            throw new SecurityException("요청 정보를 찾을 수 없습니다.");
        }

        String token = extractToken(request);
        if (token == null) {
            throw new SecurityException("토큰이 없습니다.");
        }

        if (jwtUtil.isExpired(token)) {
            throw new SecurityException("토큰이 만료되었습니다.");
        }

        log.info("🔐 인증 확인 완료: {}", joinPoint.getSignature().getName());
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
