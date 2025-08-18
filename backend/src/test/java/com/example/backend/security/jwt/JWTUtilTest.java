package com.example.backend.security.jwt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JWTUtilTest {

	@Test
	@DisplayName("createJwt로 생성한 토큰에서 클레임 추출")
	void create_and_parse_jwt_basic() {
		// given
		String secret = "dev-test-256bit-plain-secret-0123456789-ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		JWTUtil jwtUtil = new JWTUtil(secret);

		// when
		String token = jwtUtil.createJwt("access_token", "tester", "USER", 60_000L);

		// then
		assertThat(jwtUtil.getUsername(token)).isEqualTo("tester");
		assertThat(jwtUtil.getRole(token)).isEqualTo("USER");
		assertThat(jwtUtil.getCategory(token)).isEqualTo("access_token");
		assertThat(jwtUtil.isExpired(token)).isFalse();
	}

	@Test
	@DisplayName("이메일 포함 토큰에서도 클레임 추출")
	void create_and_parse_jwt_with_email() {
		String secret = "dev-test-256bit-plain-secret-0123456789-ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		JWTUtil jwtUtil = new JWTUtil(secret);

		String token = jwtUtil.createJwt("access_token", "tester", "tester@example.com", "ADMIN", 60_000L);

		assertThat(jwtUtil.getUsername(token)).isEqualTo("tester");
		assertThat(jwtUtil.getEmail(token)).isEqualTo("tester@example.com");
		assertThat(jwtUtil.getRole(token)).isEqualTo("ADMIN");
	}
}
