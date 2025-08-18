package com.example.backend.support;

import com.example.backend.order.OrderController;
import com.example.backend.order.OrderService;
import com.example.backend.security.jwt.JWTFilter;
import com.example.backend.security.jwt.JWTUtil;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 템플릿 모음 (기본 비활성화)
@Disabled("템플릿 예시 - 필요 시 복사해 사용하세요")
class ControllerTestsTemplate {
	@WebMvcTest(controllers = OrderController.class)
	@AutoConfigureMockMvc
	static class OrderControllerTemplate {
		@Autowired MockMvc mockMvc;
		@MockBean OrderService orderService;
		@MockBean JWTUtil jwtUtil; @MockBean JWTFilter jwtFilter;

		@Test @DisplayName("GET 템플릿 - 인증 사용자 200")
		@WithMockUser(username = "user@example.com", roles = {"USER"})
		void get_ok() throws Exception {
			given(orderService.findOrdersByRole(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString()))
					.willReturn(java.util.Collections.emptyList());
			mockMvc.perform(get("/orders"))
					.andExpect(status().isOk());
		}

		@Test @DisplayName("POST 템플릿 - CSRF 포함 200")
		@WithMockUser(username = "user@example.com", roles = {"USER"})
		void post_with_csrf_ok() throws Exception {
			String body = "{\n  \"memberId\": 1, \n  \"itemId\": 10, \n  \"count\": 2\n}";
			mockMvc.perform(post("/orders").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
					.andExpect(status().isOk());
		}

		@Test @DisplayName("POST 템플릿 - CSRF 없음 403")
		void post_without_csrf_forbidden() throws Exception {
			String body = "{\n  \"memberId\": 1, \n  \"itemId\": 10, \n  \"count\": 2\n}";
			mockMvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(body))
					.andExpect(status().isForbidden());
		}
	}
}

@Disabled("템플릿 예시 - 필요 시 복사해 사용하세요")
@ExtendWith(MockitoExtension.class)
class ServiceTestsTemplate {
	@InjectMocks com.example.backend.order.OrderService orderService;
	@Mock com.example.backend.order.OrderRepository orderRepository;
	@Mock com.example.backend.security.repository.UserRepository userRepository;
	@Mock com.example.backend.item.ItemRepository itemRepository;

	@Test @DisplayName("예외 템플릿 - IllegalArgumentException")
	void exception_template() {
		org.mockito.BDDMockito.given(userRepository.findById(1L)).willReturn(java.util.Optional.empty());
		org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> orderService.order(1L, 10L, 1));
	}
}

@Disabled("템플릿 예시 - 필요 시 복사해 사용하세요")
class RepositoryTestsTemplate {
	// @DataJpaTest
	// @AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
	// class Example { }
}
