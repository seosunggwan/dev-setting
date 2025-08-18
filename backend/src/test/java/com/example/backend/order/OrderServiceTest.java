package com.example.backend.order;

import com.example.backend.item.ItemRepository;
import com.example.backend.item.domain.Item;
import com.example.backend.common.exception.NotEnoughStockException;
import com.example.backend.security.entity.Role;
import com.example.backend.security.entity.UserEntity;
import com.example.backend.security.repository.UserRepository;
import com.example.backend.order.dto.PagedOrdersDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

	@InjectMocks
	private OrderService orderService;

	@Mock
	private OrderRepository orderRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private ItemRepository itemRepository;

	@Test
	@DisplayName("ADMIN은 전체 주문 조회 메서드를 사용한다")
	void findOrdersByRole_admin() {
		given(orderRepository.findAllWithMemberAndItems()).willReturn(Collections.emptyList());

		List<Order> result = orderService.findOrdersByRole(Role.ADMIN, "admin@example.com");

		assertThat(result).isEmpty();
		verify(orderRepository, times(1)).findAllWithMemberAndItems();
		verify(orderRepository, times(0)).findAllByMemberWithMemberAndItems(anyString());
	}

	@Test
	@DisplayName("USER는 본인 주문 조회 메서드를 사용한다")
	void findOrdersByRole_user() {
		given(orderRepository.findAllByMemberWithMemberAndItems("user@example.com"))
				.willReturn(Collections.emptyList());

		List<Order> result = orderService.findOrdersByRole(Role.USER, "user@example.com");

		assertThat(result).isEmpty();
		verify(orderRepository, times(0)).findAllWithMemberAndItems();
		verify(orderRepository, times(1)).findAllByMemberWithMemberAndItems("user@example.com");
	}

	@Test
	@DisplayName("ADMIN 페이징 조회: findAllWithPaging + count")
	void findOrdersByRoleWithPaging_admin() {
		given(orderRepository.findAllWithPaging(0, 10)).willReturn(Collections.emptyList());
		given(orderRepository.count()).willReturn(0L);

		PagedOrdersDto dto = orderService.findOrdersByRoleWithPaging(Role.ADMIN, "admin@example.com", 0, 10);

		assertEquals(0, dto.getOrders().size());
		assertEquals(0L, dto.getPageInfo().getTotal());
		verify(orderRepository, times(1)).findAllWithPaging(0, 10);
		verify(orderRepository, times(1)).count();
	}

	@Test
	@DisplayName("USER 페이징 조회: findAllByMemberWithPaging + countByMember")
	void findOrdersByRoleWithPaging_user() {
		given(orderRepository.findAllByMemberWithPaging("user@example.com", 0, 10))
				.willReturn(Collections.emptyList());
		given(orderRepository.countByMember("user@example.com")).willReturn(0L);

		PagedOrdersDto dto = orderService.findOrdersByRoleWithPaging(Role.USER, "user@example.com", 0, 10);

		assertEquals(0, dto.getOrders().size());
		assertEquals(0L, dto.getPageInfo().getTotal());
		verify(orderRepository, times(1)).findAllByMemberWithPaging("user@example.com", 0, 10);
		verify(orderRepository, times(1)).countByMember("user@example.com");
	}

	@Test
	@DisplayName("order() 호출 시 재고 차감 및 저장 호출")
	void order_decrease_stock_and_save() {
		// given
		UserEntity member = UserEntity.builder()
				.id(1L)
				.email("user@example.com")
				.username("user")
				.build();
		Item item = new Item();
		item.setId(10L);
		item.setName("상품");
		item.setPrice(1000);
		item.setStockQuantity(5);

		given(userRepository.findById(1L)).willReturn(Optional.of(member));
		given(itemRepository.findOne(10L)).willReturn(item);

		// when

		// then
		assertThat(item.getStockQuantity()).isEqualTo(3); // 5 -> 3
		verify(orderRepository, times(1)).save(any(Order.class));
	}

	@Test
	@DisplayName("cancelOrder() 호출 시 각 주문상품에 대해 재고 복구")
	void cancel_restores_stock() {
		// given: 주문 1건 구성
		Item item = new Item();
		item.setId(10L);
		item.setName("상품");
		item.setPrice(1000);
		item.setStockQuantity(1);

		OrderItem oi = OrderItem.createOrderItem(item, 1000, 1); // stock 1 -> 0

		UserEntity member = UserEntity.builder()
				.id(1L)
				.email("user@example.com")
				.username("user")
				.build();

		Order order = Order.createOrder(member, new com.example.backend.delivery.Delivery(), oi);
		order.setId(100L);

		given(orderRepository.findOne(100L)).willReturn(order);

		// when
		orderService.cancelOrder(100L);

		// then: 취소 후 재고 복구 (0 -> 1)
		assertThat(item.getStockQuantity()).isEqualTo(1);
	}

	// ===== 예외 케이스 =====
	@Test
	@DisplayName("order(): 회원이 없으면 IllegalArgumentException")
	void order_member_not_found() {
		given(userRepository.findById(1L)).willReturn(Optional.empty());

		assertThrows(IllegalArgumentException.class, () -> orderService.order(1L, 10L, 1));
	}

	@Test
	@DisplayName("order(): 재고 부족이면 NotEnoughStockException")
	void order_not_enough_stock() {
		UserEntity member = UserEntity.builder().id(1L).email("u@e.com").username("u").build();
		Item item = new Item();
		item.setId(10L);
		item.setName("상품");
		item.setPrice(1000);
		item.setStockQuantity(0); // 재고 0

		given(userRepository.findById(1L)).willReturn(Optional.of(member));
		given(itemRepository.findOne(10L)).willReturn(item);

		assertThrows(NotEnoughStockException.class, () -> orderService.order(1L, 10L, 1));
	}

	@Test
	@DisplayName("cancelOrder(): 배송완료 상태면 IllegalStateException")
	void cancel_delivery_completed() {
		// 주문 구성
		UserEntity member = UserEntity.builder().id(1L).email("u@e.com").username("u").build();
		Item item = new Item(); item.setId(10L); item.setName("상품"); item.setPrice(1000); item.setStockQuantity(1);
		OrderItem oi = OrderItem.createOrderItem(item, 1000, 1);
		Order order = Order.createOrder(member, new com.example.backend.delivery.Delivery(), oi);
		order.getDelivery().setStatus(com.example.backend.delivery.DeliveryStatus.COMP); // 배송완료
		order.setId(200L);
		given(orderRepository.findOne(200L)).willReturn(order);

		assertThrows(IllegalStateException.class, () -> orderService.cancelOrder(200L));
	}

	@Nested
	@DisplayName("검색/카운트 위임 검증")
	class SearchAndCount {
		@Test
		void count_all() {
			given(orderRepository.count()).willReturn(42L);
			assertThat(orderService.countOrders()).isEqualTo(42L);
		}

		@Test
		void count_by_search() {
			OrderSearch s = new OrderSearch();
			given(orderRepository.countBySearch(s)).willReturn(7L);
			assertThat(orderService.countOrders(s)).isEqualTo(7L);
		}
	}
}
