package com.example.backend.security.util;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CookieUtilTest {

	@Test
	@DisplayName("createCookie: 기본 속성 검증")
	void createCookie_ok() {
		Cookie cookie = CookieUtil.createCookie("refresh", "abc", 3600);
		assertThat(cookie.getName()).isEqualTo("refresh");
		assertThat(cookie.getValue()).isEqualTo("abc");
		assertThat(cookie.getMaxAge()).isEqualTo(3600);
		assertThat(cookie.isHttpOnly()).isTrue();
		assertThat(cookie.getPath()).isEqualTo("/");
		assertThat(cookie.getSecure()).isTrue();
	}

	@Test
	@DisplayName("deleteCookie: 삭제 속성 검증")
	void deleteCookie_ok() {
		Cookie cookie = CookieUtil.deleteCookie("refresh");
		assertThat(cookie.getName()).isEqualTo("refresh");
		assertThat(cookie.getValue()).isNull();
		assertThat(cookie.getMaxAge()).isZero();
		assertThat(cookie.isHttpOnly()).isTrue();
		assertThat(cookie.getPath()).isEqualTo("/");
		assertThat(cookie.getSecure()).isTrue();
	}
}
