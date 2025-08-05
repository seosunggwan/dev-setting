package com.example.backend.board.controller;

import com.example.backend.board.service.PopularBoardService;
import com.example.backend.board.dto.PopularBoardDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

/**
 * PopularBoardController Spring Security 테스트
 * - @PreAuthorize("hasRole('ADMIN')") 어노테이션 테스트
 * - 관리자 권한과 일반 사용자 권한 구분 테스트
 * - MockMvc를 사용한 컨트롤러 레이어 단위 테스트
 */
@SpringBootTest(
    exclude = {RedisAutoConfiguration.class},
    webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PopularBoardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PopularBoardService popularBoardService; // PopularBoardService 모킹

    @Test
    @DisplayName("인증없이 오늘의 인기글 조회 시 로그인 리다이렉트")
    void 인증없이_오늘인기글_조회시_로그인_리다이렉트() throws Exception {
        // when & then
        mockMvc.perform(get("/api/boards/popular/today"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = {"USER"})
    @DisplayName("인증된 사용자의 오늘의 인기글 조회 성공")
    void 인증된_사용자_오늘인기글_조회_성공() throws Exception {
        // given
        List<PopularBoardDto> popularBoards = new ArrayList<>();
        when(popularBoardService.getTodayPopularBoards()).thenReturn(popularBoards);

        // when & then
        mockMvc.perform(get("/api/boards/popular/today"))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = {"USER"})
    @DisplayName("일반 사용자가 수동 실행 시도 시 403 Forbidden 반환")
    void 일반사용자_수동실행_시도시_403_반환() throws Exception {
        // when & then
        mockMvc.perform(post("/api/boards/popular/refresh"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = {"ADMIN"})
    @DisplayName("관리자의 수동 실행 성공")
    void 관리자_수동실행_성공() throws Exception {
        // given
        doNothing().when(popularBoardService).runManualPopularBoardSelection();

        // when & then
        mockMvc.perform(post("/api/boards/popular/refresh"))
                .andExpect(status().isOk())
;
    }

    @Test
    @DisplayName("인증없이 수동 실행 시도 시 로그인 리다이렉트")
    void 인증없이_수동실행_시도시_로그인_리다이렉트() throws Exception {
        // when & then
        mockMvc.perform(post("/api/boards/popular/refresh"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = {"USER"})
    @DisplayName("일반 사용자가 계산 실행 시도 시 403 Forbidden 반환")
    void 일반사용자_계산실행_시도시_403_반환() throws Exception {
        // when & then
        mockMvc.perform(post("/api/boards/popular/calculate"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = {"ADMIN"})
    @DisplayName("관리자의 계산 실행 성공")
    void 관리자_계산실행_성공() throws Exception {
        // given
        doNothing().when(popularBoardService).selectDailyPopularBoards();

        // when & then
        mockMvc.perform(post("/api/boards/popular/calculate"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = {"USER"})
    @DisplayName("인증된 사용자의 특정 날짜 인기글 조회 성공")
    void 인증된_사용자_특정날짜_인기글_조회_성공() throws Exception {
        // given
        LocalDate date = LocalDate.now().minusDays(1);
        List<PopularBoardDto> popularBoards = new ArrayList<>();
        when(popularBoardService.getPopularBoardsByDate(any(LocalDate.class))).thenReturn(popularBoards);

        // when & then
        mockMvc.perform(get("/api/boards/popular/date/" + date.toString()))
                .andExpect(status().isOk())
;
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = {"USER"})
    @DisplayName("인증된 사용자의 최근 N일간 인기글 조회 성공")
    void 인증된_사용자_최근N일간_인기글_조회_성공() throws Exception {
        // given
        List<PopularBoardDto> popularBoards = new ArrayList<>();
        when(popularBoardService.getRecentPopularBoards(anyInt())).thenReturn(popularBoards);

        // when & then
        mockMvc.perform(get("/api/boards/popular/recent")
                .param("days", "7"))
                .andExpect(status().isOk())
;
    }

    @Test
    @DisplayName("인증없이 특정 날짜 인기글 조회 시 로그인 리다이렉트")
    void 인증없이_특정날짜_인기글_조회시_로그인_리다이렉트() throws Exception {
        // given
        LocalDate date = LocalDate.now().minusDays(1);

        // when & then
        mockMvc.perform(get("/api/boards/popular/date/" + date.toString()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    @DisplayName("인증없이 최근 N일간 인기글 조회 시 로그인 리다이렉트")
    void 인증없이_최근N일간_인기글_조회시_로그인_리다이렉트() throws Exception {
        // when & then
        mockMvc.perform(get("/api/boards/popular/recent")
                .param("days", "7"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("http://localhost/login"));
    }
}