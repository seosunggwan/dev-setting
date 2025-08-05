package com.example.backend.integration;

import com.example.backend.security.jwt.JWTUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ItemController JWT 통합 테스트
 * - 실제 JWT 토큰을 사용한 인증 테스트
 * - Spring Boot 전체 컨텍스트를 로드한 통합 테스트
 * - H2 인메모리 데이터베이스 사용
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional // 각 테스트 후 롤백하여 데이터베이스 상태 초기화
class ItemControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JWTUtil jwtUtil; // 실제 JWT 유틸리티 사용

    @Test
    @DisplayName("유효한 JWT 토큰으로 상품 목록 조회 성공")
    void 유효한_JWT_토큰으로_상품목록_조회_성공() throws Exception {
        // given
        String email = "test@example.com";
        String role = "USER";
        long expireMs = 60 * 60 * 1000L; // 1시간
        String token = jwtUtil.createJwt("access", email, role, expireMs);

        // when & then
        mockMvc.perform(get("/api/items/page")
                .header("Authorization", "Bearer " + token)
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("유효한 JWT 토큰으로 상품 검색 성공")
    void 유효한_JWT_토큰으로_상품검색_성공() throws Exception {
        // given
        String email = "test@example.com";
        String role = "USER";
        long expireMs = 60 * 60 * 1000L; // 1시간
        String token = jwtUtil.createJwt("access", email, role, expireMs);

        // when & then
        mockMvc.perform(get("/api/items/search")
                .header("Authorization", "Bearer " + token)
                .param("keyword", "테스트")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("잘못된 JWT 토큰으로 요청 시 401 Unauthorized 반환")
    void 잘못된_JWT_토큰으로_요청시_401_반환() throws Exception {
        // given
        String invalidToken = "invalid.jwt.token";

        // when & then
        mockMvc.perform(get("/api/items/page")
                .header("Authorization", "Bearer " + invalidToken)
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("만료된 JWT 토큰으로 요청 시 401 Unauthorized 반환")
    void 만료된_JWT_토큰으로_요청시_401_반환() throws Exception {
        // given - 이미 만료된 토큰 생성 (expireMs를 음수로 설정)
        String email = "test@example.com";
        String role = "USER";
        long expireMs = -1000L; // 이미 만료된 토큰
        String expiredToken = jwtUtil.createJwt("access", email, role, expireMs);

        // when & then
        mockMvc.perform(get("/api/items/page")
                .header("Authorization", "Bearer " + expiredToken)
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Authorization 헤더 없이 요청 시 401 Unauthorized 반환")
    void Authorization_헤더_없이_요청시_401_반환() throws Exception {
        // when & then
        mockMvc.perform(get("/api/items/page")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Bearer 접두사 없는 토큰으로 요청 시 401 Unauthorized 반환")
    void Bearer_접두사_없는_토큰으로_요청시_401_반환() throws Exception {
        // given
        String email = "test@example.com";
        String role = "USER";
        long expireMs = 60 * 60 * 1000L;
        String token = jwtUtil.createJwt("access", email, role, expireMs);

        // when & then - Bearer 접두사 없이 토큰만 전송
        mockMvc.perform(get("/api/items/page")
                .header("Authorization", token) // Bearer 접두사 없음
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("유효한 JWT 토큰으로 상품 등록 성공")
    void 유효한_JWT_토큰으로_상품등록_성공() throws Exception {
        // given
        String email = "test@example.com";
        String role = "USER";
        long expireMs = 60 * 60 * 1000L;
        String token = jwtUtil.createJwt("access", email, role, expireMs);

        String itemJson = """
                {
                    "name": "테스트 상품",
                    "price": 10000,
                    "stockQuantity": 100,
                    "imageUrl": "https://example.com/image.jpg"
                }
                """;

        // when & then
        mockMvc.perform(post("/api/items")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(itemJson))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("유효한 JWT 토큰으로 상품 수정 성공")
    void 유효한_JWT_토큰으로_상품수정_성공() throws Exception {
        // given
        String email = "test@example.com";
        String role = "USER";
        long expireMs = 60 * 60 * 1000L;
        String token = jwtUtil.createJwt("access", email, role, expireMs);

        String itemJson = """
                {
                    "name": "수정된 상품",
                    "price": 15000,
                    "stockQuantity": 50,
                    "imageUrl": "https://example.com/updated-image.jpg"
                }
                """;

        // when & then
        mockMvc.perform(put("/api/items/1")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(itemJson))
                .andExpect(status().isOk());
    }
}