package com.example.backend.order;

import com.example.backend.item.ItemService;
import com.example.backend.item.domain.Item;
import com.example.backend.order.dto.OrderFormData;
import com.example.backend.order.dto.OrderRequest;
import com.example.backend.security.entity.Address;
import com.example.backend.security.entity.UserEntity;
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
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = OrderController.class)
@AutoConfigureMockMvc
class OrderControllerApiTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private OrderService orderService;

	@MockBean
	private UserRepository userRepository;

	@MockBean
	private ItemService itemService;

	// 보안 필터 의존성 (JWT) 목 처리
	@MockBean
	private JWTUtil jwtUtil;
	@MockBean
	private JWTFilter jwtFilter;

	@Test
	@DisplayName("USER - 주문 목록 조회 200")
	@WithMockUser(username = "user@example.com", roles = {"USER"})
	void getOrders_user_ok() throws Exception {
		given(orderService.findOrdersByRole(any(), anyString())).willReturn(Collections.emptyList());

		mockMvc.perform(get("/orders"))
				.andExpect(status().isOk());
	}

	@Test
	@DisplayName("USER - 주문 상세 조회 200")
	@WithMockUser(username = "user@example.com", roles = {"USER"})
	void getOrder_ok() throws Exception {
		// 최소 구성의 주문 객체 스텁
		UserEntity member = UserEntity.builder().id(1L).email("user@example.com").username("user").address(new Address()).build();
		Item item = new Item(); item.setId(10L); item.setName("상품"); item.setPrice(1000); item.setStockQuantity(5);
		OrderItem oi = OrderItem.createOrderItem(item, 1000, 1);
		Order order = Order.createOrder(member, new com.example.backend.delivery.Delivery(), oi);
		order.setId(100L);
		given(orderService.findOrderWithMemberAndItems(100L)).willReturn(order);

		mockMvc.perform(get("/orders/100"))
				.andExpect(status().isOk());
	}

	@Test
	@DisplayName("USER - 주문 생성 200 (CSRF 포함)")
	@WithMockUser(username = "user@example.com", roles = {"USER"})
	void createOrder_ok() throws Exception {
		String body = "{\n  \"memberId\": 1, \n  \"itemId\": 10, \n  \"count\": 2\n}";
		given(orderService.order(1L, 10L, 2)).willReturn(999L);
		given(orderService.findOrderWithMemberAndItems(999L)).willReturn(new Order());

		mockMvc.perform(post("/orders")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
				.andExpect(status().isOk());
	}

	@Test
	@DisplayName("USER - 주문 폼 데이터 200")
	@WithMockUser(username = "user@example.com", roles = {"USER"})
	void getForm_ok() throws Exception {
		UserEntity member = UserEntity.builder().id(1L).email("user@example.com").username("user").build();
		given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(member));
		given(itemService.findItems()).willReturn(List.of());

		mockMvc.perform(get("/orders/form"))
				.andExpect(status().isOk());
	}

	@Test
	@DisplayName("USER - 주문 검색 200")
	@WithMockUser(username = "user@example.com", roles = {"USER"})
	void searchOrders_ok() throws Exception {
		given(orderService.findOrdersByRoleAndSearch(any(), anyString(), any(OrderSearch.class)))
				.willReturn(Collections.emptyList());

		mockMvc.perform(get("/orders/search")
				.param("itemName", "상품")
				.param("categoryName", "도서"))
				.andExpect(status().isOk());
	}
}
