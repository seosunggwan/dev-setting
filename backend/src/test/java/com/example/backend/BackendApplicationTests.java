package com.example.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test") // application-test.yml 적용
class BackendApplicationTests {

    @Test
    void contextLoads() {
        // 스프링 컨텍스트 로딩만 검증
    }
}
