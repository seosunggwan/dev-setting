package com.example.backend.integration;

import com.example.backend.security.jwt.JWTUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Spring Security 전체 통합 테스트
 * - 다양한 엔드포인트들의 보안 설정 검증
 * - JWT 토큰 기반 인증 테스트
 * - 권한별 접근 제어 테스트
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JWTUtil jwtUtil;

    @Test
    @DisplayName("PUBLIC 엔드포인트 접근 테스트 - 인증 없이 접근 가능")
    void PUBLIC_엔드포인트_접근_테스트() throws Exception {
        // Health check 엔드포인트는 인증 없이 접근 가능해야 함
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("인증이 필요한 엔드포인트 - 토큰 없이 접근 시 401")
    void 인증필요_엔드포인트_토큰없이_접근시_401() throws Exception {
        mockMvc.perform(get("/api/items"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("USER 권한으로 일반 보호된 리소스 접근 성공")
    void USER_권한으로_일반보호된_리소스_접근_성공() throws Exception {
        // given
        String token = jwtUtil.createJwt("access", "user@example.com", "USER", 60 * 60 * 1000L);

        // when & then
        mockMvc.perform(get("/api/items")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("USER 권한으로 ADMIN 리소스 접근 시 403 Forbidden")
    void USER_권한으로_ADMIN_리소스_접근시_403() throws Exception {
        // given
        String token = jwtUtil.createJwt("access", "user@example.com", "USER", 60 * 60 * 1000L);

        // when & then
        mockMvc.perform(post("/api/popular-boards/manual-run")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMIN 권한으로 관리자 리소스 접근 성공")
    void ADMIN_권한으로_관리자_리소스_접근_성공() throws Exception {
        // given
        String token = jwtUtil.createJwt("access", "admin@example.com", "ADMIN", 60 * 60 * 1000L);

        // when & then
        mockMvc.perform(post("/api/popular-boards/manual-run")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("ADMIN 권한으로 일반 사용자 리소스도 접근 가능")
    void ADMIN_권한으로_일반사용자_리소스_접근_가능() throws Exception {
        // given
        String token = jwtUtil.createJwt("access", "admin@example.com", "ADMIN", 60 * 60 * 1000L);

        // when & then
        mockMvc.perform(get("/api/items")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("잘못된 JWT 토큰 형식으로 요청 시 401")
    void 잘못된_JWT_토큰_형식으로_요청시_401() throws Exception {
        mockMvc.perform(get("/api/items")
                .header("Authorization", "InvalidToken"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("만료된 토큰으로 요청 시 401")
    void 만료된_토큰으로_요청시_401() throws Exception {
        // given - 과거 시간으로 만료된 토큰 생성
        String expiredToken = jwtUtil.createJwt("access", "user@example.com", "USER", -1000L);

        // when & then
        mockMvc.perform(get("/api/items")
                .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("빈 Authorization 헤더로 요청 시 401")
    void 빈_Authorization_헤더로_요청시_401() throws Exception {
        mockMvc.perform(get("/api/items")
                .header("Authorization", ""))
                .andExpect(status().isUnauthorized());
    }
}