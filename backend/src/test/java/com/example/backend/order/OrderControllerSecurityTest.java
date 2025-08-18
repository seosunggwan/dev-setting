package com.example.backend.order;

import com.example.backend.item.ItemService;
import com.example.backend.security.jwt.JWTFilter;
import com.example.backend.security.jwt.JWTUtil;
import com.example.backend.security.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = OrderController.class)
@AutoConfigureMockMvc
class OrderControllerSecurityTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private OrderService orderService;

	@MockBean
	private UserRepository userRepository;

	@MockBean
	private ItemService itemService;

	// 보안 필터 의존성 해결
	@MockBean
	private JWTUtil jwtUtil;

	@MockBean
	private JWTFilter jwtFilter;

	@Test
	@WithMockUser(username = "user@example.com", roles = {"USER"})
	@DisplayName("주문 목록 접근 200")
	void list_without_auth_unauthorized() throws Exception {
		given(orderService.findOrdersByRole(any(), anyString())).willReturn(Collections.emptyList());

		mockMvc.perform(get("/orders"))
				.andExpect(status().isOk());
	}

	@Test
	@WithMockUser(username = "user@example.com", roles = {"USER"})
	@DisplayName("USER 권한으로 주문 목록 접근 200")
	void list_with_user_ok() throws Exception {
		given(orderService.findOrdersByRole(any(), anyString())).willReturn(Collections.emptyList());

		mockMvc.perform(get("/orders"))
				.andExpect(status().isOk());
	}

	@Test
	@DisplayName("CSRF 없이 POST는 403")
	void post_without_csrf_forbidden() throws Exception {
		String body = "{\n  \"memberId\": 1, \n  \"itemId\": 1, \n  \"count\": 1\n}";
		mockMvc.perform(post("/orders")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = "user@example.com", roles = {"USER"})
	@DisplayName("CSRF 포함 POST는 보안 통과(4xx여도 403은 아님)")
	void post_with_csrf_not_forbidden() throws Exception {
		String body = "{\n  \"memberId\": 1, \n  \"itemId\": 1, \n  \"count\": 1\n}";
		mockMvc.perform(post("/orders")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
				.andExpect(result -> {
					int status = result.getResponse().getStatus();
					if (status == 403) {
						throw new AssertionError("CSRF 포함인데 403이 나왔습니다.");
					}
				});
	}
}
