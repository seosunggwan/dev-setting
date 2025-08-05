package com.example.backend.simple;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 간단한 테스트 - Spring Boot 컨텍스트 로딩 확인
 */
@SpringBootTest
@ActiveProfiles("test")
class SimpleControllerTest {

    @Test
    @DisplayName("Spring Boot 컨텍스트가 정상적으로 로드되는지 확인")
    void contextLoads() {
        // Spring Boot 애플리케이션 컨텍스트가 정상적으로 로드되는지만 확인
        assertTrue(true);
    }
}