package com.example.backend.item.controller;

import com.example.backend.item.ItemService;
import com.example.backend.item.domain.Item;
import com.example.backend.item.dto.PagedItemsDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ItemController Spring Security 테스트
 * - @PreAuthorize("isAuthenticated()") 어노테이션 테스트
 * - 인증된 사용자와 비인증 사용자 구분 테스트
 * - MockMvc를 사용한 컨트롤러 레이어 단위 테스트
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ItemService itemService; // ItemService 모킹

    @Test
    @DisplayName("인증없이 상품 목록 조회 시 401 Unauthorized 반환")
    void 인증없이_상품목록_조회시_401_반환() throws Exception {
        // when & then
        mockMvc.perform(get("/api/items"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("인증없이 페이지네이션 상품 목록 조회 시 401 Unauthorized 반환")
    void 인증없이_페이지네이션_상품목록_조회시_401_반환() throws Exception {
        // when & then
        mockMvc.perform(get("/api/items/page")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = {"USER"})
    @DisplayName("인증된 사용자의 상품 목록 조회 성공")
    void 인증된_사용자_상품목록_조회_성공() throws Exception {
        // given
        List<Item> items = new ArrayList<>();
        PagedItemsDto pagedItems = new PagedItemsDto(items, 0, 10, 0);
        when(itemService.findItemsWithPaging(anyInt(), anyInt())).thenReturn(pagedItems);

        // when & then
        mockMvc.perform(get("/api/items/page")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = {"USER"})
    @DisplayName("인증된 사용자의 상품 검색 성공")
    void 인증된_사용자_상품검색_성공() throws Exception {
        // given
        List<Item> items = new ArrayList<>();
        PagedItemsDto pagedItems = new PagedItemsDto(items, 0, 10, 0);
        when(itemService.searchItemsWithPaging(anyString(), anyInt(), anyInt())).thenReturn(pagedItems);

        // when & then
        mockMvc.perform(get("/api/items/search")
                .param("keyword", "테스트")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("인증없이 상품 검색 시 401 Unauthorized 반환")
    void 인증없이_상품검색시_401_반환() throws Exception {
        // when & then
        mockMvc.perform(get("/api/items/search")
                .param("keyword", "테스트")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = {"USER"})
    @DisplayName("인증된 사용자의 상품 등록 성공")
    void 인증된_사용자_상품등록_성공() throws Exception {
        // given
        when(itemService.saveItem(any())).thenReturn(1L);
        
        String itemJson = """
                {
                    "name": "테스트 상품",
                    "price": 10000,
                    "stockQuantity": 100
                }
                """;

        // when & then
        mockMvc.perform(post("/api/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(itemJson))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("인증없이 상품 등록 시 401 Unauthorized 반환")
    void 인증없이_상품등록시_401_반환() throws Exception {
        // given
        String itemJson = """
                {
                    "name": "테스트 상품",
                    "price": 10000,
                    "stockQuantity": 100
                }
                """;

        // when & then
        mockMvc.perform(post("/api/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(itemJson))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = {"USER"})
    @DisplayName("인증된 사용자의 상품 수정 성공")
    void 인증된_사용자_상품수정_성공() throws Exception {
        // given
        String itemJson = """
                {
                    "name": "수정된 상품",
                    "price": 15000,
                    "stockQuantity": 50
                }
                """;

        // when & then
        mockMvc.perform(put("/api/items/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(itemJson))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("인증없이 상품 수정 시 401 Unauthorized 반환")
    void 인증없이_상품수정시_401_반환() throws Exception {
        // given
        String itemJson = """
                {
                    "name": "수정된 상품",
                    "price": 15000,
                    "stockQuantity": 50
                }
                """;

        // when & then
        mockMvc.perform(put("/api/items/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(itemJson))
                .andExpect(status().isUnauthorized());
    }
}