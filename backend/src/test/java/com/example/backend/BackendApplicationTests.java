package com.example.backend;

import com.example.backend.annotation.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 메인 애플리케이션 테스트 클래스
 * - H2 인메모리 데이터베이스 사용
 * - 테스트 프로파일 활성화
 * - Spring Boot 컨텍스트 로딩 테스트
 */
@IntegrationTest
class BackendApplicationTests {

	@Test
	void contextLoads() {
		// Spring Boot 애플리케이션 컨텍스트가 정상적으로 로드되는지 확인
		// 이 테스트는 다음을 검증합니다:
		// 1. 모든 Bean이 정상적으로 생성되는지
		// 2. 의존성 주입이 올바르게 이루어지는지
		// 3. 설정 파일이 올바르게 로드되는지
		// 4. H2 데이터베이스 연결이 정상적으로 이루어지는지
	}

	@Test
	void applicationContextTest() {
		// 추가적인 컨텍스트 검증 테스트
		// 실제 프로젝트에서는 여기에 더 구체적인 테스트 로직을 추가할 수 있습니다
	}

}
