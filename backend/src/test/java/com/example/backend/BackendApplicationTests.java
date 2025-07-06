package com.example.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class BackendApplicationTests {

	@Test
	void contextLoads() {
		// 이 테스트는 Spring 컨텍스트가 올바르게 로드되는지 확인합니다.
		// H2 인메모리 데이터베이스와 테스트 설정을 사용합니다.
	}

}
