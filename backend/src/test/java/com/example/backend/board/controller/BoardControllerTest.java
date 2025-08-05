package com.example.backend.board.controller;

import com.example.backend.board.dto.BoardDto;
import com.example.backend.board.dto.PagedBoardsDto;
import com.example.backend.board.service.BoardService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BoardController.class)
@ActiveProfiles("test")
class BoardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper(); // ✅ 변경: 직접 생성

    @MockBean
    private BoardService boardService;
    
    // ✅ Security 관련 빈들 모킹 (JWT 의존성 해결)
    @MockBean
    private com.example.backend.security.jwt.JWTUtil jwtUtil;
    
    @MockBean
    private com.example.backend.security.service.form.CustomUserDetailsService customUserDetailsService;

    private BoardDto.CreateRequest createRequest;
    private BoardDto.UpdateRequest updateRequest;
    private BoardDto.Response boardResponse;
    private BoardDto.ListResponse listResponse;
    private PagedBoardsDto pagedResponse;

    @BeforeEach
    void setUp() {
        createRequest = BoardDto.CreateRequest.builder()
                .title("테스트 게시글 제목")
                .content("테스트 게시글 내용입니다. 10자 이상이어야 합니다.")
                .build();

        updateRequest = BoardDto.UpdateRequest.builder()
                .title("수정된 게시글 제목")
                .content("수정된 게시글 내용입니다. 10자 이상이어야 합니다.")
                .build();

        boardResponse = BoardDto.Response.builder()
                .id(1L)
                .title("테스트 게시글 제목")
                .content("테스트 게시글 내용입니다.")
                .authorName("testuser")
                .createdTime(LocalDateTime.now())
                .updatedTime(LocalDateTime.now())
                .viewCount(0)
                .likeCount(0)
                .isAuthor(true)
                .isLiked(false)
                .build();

        listResponse = BoardDto.ListResponse.builder()
                .id(1L)
                .title("테스트 게시글 제목")
                .authorName("testuser")
                .createdTime(LocalDateTime.now())
                .viewCount(0)
                .likeCount(0)
                .commentCount(0)
                .build();

        PagedBoardsDto.PageInfo pageInfo = new PagedBoardsDto.PageInfo(
                0, 10, 1L, 1, true, true, false, false
        );
        pagedResponse = new PagedBoardsDto(Arrays.asList(listResponse), pageInfo);
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    @DisplayName("✅ USER 권한으로 게시글 생성 성공")
    void createBoard_WithAuthenticatedUser_ShouldSucceed() throws Exception {
        when(boardService.create(any(BoardDto.CreateRequest.class), eq("testuser")))
                .thenReturn(boardResponse);

        mockMvc.perform(post("/boards")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("테스트 게시글 제목"))
                .andExpect(jsonPath("$.authorName").value("testuser"));

        verify(boardService, times(1)).create(any(BoardDto.CreateRequest.class), eq("testuser"));
    }

    @Test
    @DisplayName("❌ 인증되지 않은 사용자의 게시글 생성 실패 (302 리다이렉트)")
    void createBoard_WithoutAuthentication_ShouldRedirect() throws Exception {
        mockMvc.perform(post("/boards")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andDo(print())
                .andExpect(status().isFound());  // ✅ 302 Found (로그인 페이지로 리다이렉트)

        verify(boardService, never()).create(any(), any());
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("❌ 유효하지 않은 데이터로 게시글 생성 실패")
    void createBoard_WithInvalidData_ShouldFail() throws Exception {
        BoardDto.CreateRequest invalidRequest = BoardDto.CreateRequest.builder()
                .title("")
                .content("내용")
                .build();

        mockMvc.perform(post("/boards")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(boardService, never()).create(any(), any());
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("✅ 게시글 수정 성공")
    void updateBoard_ShouldSucceed() throws Exception {
        Long boardId = 1L;
        BoardDto.Response updatedResponse = BoardDto.Response.builder()
                .id(boardId)
                .title("수정된 게시글 제목")
                .content("수정된 게시글 내용입니다.")
                .authorName("testuser")
                .createdTime(LocalDateTime.now())
                .updatedTime(LocalDateTime.now())
                .viewCount(0)
                .likeCount(0)
                .isAuthor(true)
                .isLiked(false)
                .build();

        when(boardService.update(eq(boardId), any(BoardDto.UpdateRequest.class), eq("testuser")))
                .thenReturn(updatedResponse);

        mockMvc.perform(put("/boards/{boardId}", boardId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("수정된 게시글 제목"));

        verify(boardService, times(1)).update(eq(boardId), any(BoardDto.UpdateRequest.class), eq("testuser"));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("✅ 게시글 삭제 성공")
    void deleteBoard_ShouldSucceed() throws Exception {
        Long boardId = 1L;
        doNothing().when(boardService).delete(boardId, "testuser");

        mockMvc.perform(delete("/boards/{boardId}", boardId)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isNoContent());

        verify(boardService, times(1)).delete(boardId, "testuser");
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})  // ✅ 인증 추가
    @DisplayName("✅ 게시글 상세 조회 성공")
    void getBoardDetail_ShouldSucceed() throws Exception {
        Long boardId = 1L;
        when(boardService.getBoard(boardId)).thenReturn(boardResponse);

        mockMvc.perform(get("/boards/{boardId}", boardId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("테스트 게시글 제목"));

        verify(boardService, times(1)).getBoard(boardId);
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})  // ✅ 인증 추가
    @DisplayName("✅ 전체 게시글 목록 조회 성공")
    void getAllBoards_ShouldSucceed() throws Exception {
        List<BoardDto.ListResponse> boardList = Arrays.asList(listResponse);
        when(boardService.getAllBoards()).thenReturn(boardList);

        mockMvc.perform(get("/boards"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1L));

        verify(boardService, times(1)).getAllBoards();
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})  // ✅ 인증 추가
    @DisplayName("✅ 페이지네이션 게시글 목록 조회 성공")
    void getBoardsWithPaging_ShouldSucceed() throws Exception {
        when(boardService.getBoardsWithPaging(0, 10)).thenReturn(pagedResponse);

        mockMvc.perform(get("/boards/page")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.boards").isArray())
                .andExpect(jsonPath("$.pageInfo.total").value(1))
                .andExpect(jsonPath("$.pageInfo.totalPages").value(1))
                .andExpect(jsonPath("$.pageInfo.page").value(0));

        verify(boardService, times(1)).getBoardsWithPaging(0, 10);
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})  // ✅ 인증 추가
    @DisplayName("✅ 페이지 크기가 50을 초과할 때 자동으로 50으로 제한")
    void getBoardsWithPaging_WithLargeSizeLimit_ShouldLimitTo50() throws Exception {
        when(boardService.getBoardsWithPaging(0, 50)).thenReturn(pagedResponse);

        mockMvc.perform(get("/boards/page")
                        .param("page", "0")
                        .param("size", "100"))
                .andDo(print())
                .andExpect(status().isOk());

        verify(boardService, times(1)).getBoardsWithPaging(0, 50);
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})  // ✅ 인증 추가
    @DisplayName("✅ 키워드로 게시글 검색 성공")
    void searchBoardsByKeyword_ShouldSucceed() throws Exception {
        String keyword = "테스트";
        when(boardService.searchBoardsByKeyword(keyword, 0, 10)).thenReturn(pagedResponse);

        mockMvc.perform(get("/boards/search")
                        .param("keyword", keyword)
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.boards").isArray());

        verify(boardService, times(1)).searchBoardsByKeyword(keyword, 0, 10);
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})  // ✅ 인증 추가
    @DisplayName("✅ 작성자로 게시글 검색 성공")
    void searchBoardsByAuthor_ShouldSucceed() throws Exception {
        String authorName = "testuser";
        when(boardService.searchBoardsByAuthor(authorName, 0, 10)).thenReturn(pagedResponse);

        mockMvc.perform(get("/boards/search/author")
                        .param("authorName", authorName)
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.boards").isArray());

        verify(boardService, times(1)).searchBoardsByAuthor(authorName, 0, 10);
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})  // ✅ 인증 추가
    @DisplayName("✅ 키워드 없이 검색 시에도 정상 처리")
    void searchBoards_WithoutKeyword_ShouldSucceed() throws Exception {
        when(boardService.searchBoardsByKeyword(null, 0, 10)).thenReturn(pagedResponse);

        mockMvc.perform(get("/boards/search")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk());

        verify(boardService, times(1)).searchBoardsByKeyword(null, 0, 10);
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("❌ CSRF 토큰 없이 POST 요청 시 실패")
    void createBoard_WithoutCsrfToken_ShouldFail() throws Exception {
        mockMvc.perform(post("/boards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andDo(print())
                .andExpect(status().isForbidden());

        verify(boardService, never()).create(any(), any());
    }
    
    // ===== 🎯 권한별 심화 테스트 케이스 =====
    
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("✅ ADMIN 권한으로 모든 게시글 관리 가능")
    void createBoard_WithAdminRole_ShouldSucceed() throws Exception {
        // given
        when(boardService.create(any(BoardDto.CreateRequest.class), eq("admin")))
                .thenReturn(boardResponse);

        // when & then - ADMIN은 모든 게시글 CRUD 가능
        mockMvc.perform(post("/boards")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("테스트 게시글 제목"));

        verify(boardService, times(1)).create(any(BoardDto.CreateRequest.class), eq("admin"));
    }
    
    @Test
    @WithMockUser(username = "user1", roles = {"USER"})
    @DisplayName("❌ 다른 사용자의 게시글 수정 시도 - 권한 검증")
    void updateBoard_WithDifferentUser_ShouldThrowException() throws Exception {
        // given - 다른 사용자가 작성한 게시글을 수정하려고 시도
        Long boardId = 1L;
        doThrow(new RuntimeException("작성자만 수정할 수 있습니다"))
                .when(boardService).update(eq(boardId), any(BoardDto.UpdateRequest.class), eq("user1"));

        // when & then - 권한 오류 발생 (RuntimeException으로 500 에러)
        mockMvc.perform(put("/boards/{boardId}", boardId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andDo(print())
                .andExpect(status().isInternalServerError());  // ✅ 500 Internal Server Error (RuntimeException)

        verify(boardService, times(1)).update(eq(boardId), any(BoardDto.UpdateRequest.class), eq("user1"));
    }
    
    @Test
    @WithMockUser(username = "moderator", roles = {"MODERATOR"})
    @DisplayName("✅ MODERATOR 권한으로 게시글 조회는 가능")
    void getBoard_WithModeratorRole_ShouldSucceed() throws Exception {
        // given
        Long boardId = 1L;
        when(boardService.getBoard(boardId)).thenReturn(boardResponse);

        // when & then - MODERATOR는 조회 가능
        mockMvc.perform(get("/boards/{boardId}", boardId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("테스트 게시글 제목"));

        verify(boardService, times(1)).getBoard(boardId);
    }
    
    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    @DisplayName("✅ 본인 게시글 수정/삭제 권한 확인")
    void deleteBoard_AsOwner_ShouldSucceed() throws Exception {
        // given
        Long boardId = 1L;
        doNothing().when(boardService).delete(boardId, "testuser");

        // when & then - 본인 게시글은 삭제 가능
        mockMvc.perform(delete("/boards/{boardId}", boardId)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isNoContent());  // ✅ 삭제는 204 No Content

        verify(boardService, times(1)).delete(boardId, "testuser");
    }
}
