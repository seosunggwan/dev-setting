package com.example.backend.integration;
import com.example.backend.item.ItemRepository;
import com.example.backend.item.domain.Item;
import com.example.backend.order.Order;
import com.example.backend.order.OrderItem;
import com.example.backend.order.OrderService;
import com.example.backend.security.entity.Address;
import com.example.backend.security.entity.UserEntity;
import com.example.backend.security.repository.UserRepository;
import com.example.backend.chat.service.RedisPubSubService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
    "spring.cloud.aws.credentials.access-key=test-access-key",
    "spring.cloud.aws.credentials.secret-key=test-secret-key",
    "spring.cloud.aws.region.static=ap-northeast-2",
    "spring.cloud.aws.s3.bucket=test-bucket",
    "spring.jwt.secret=dev-test-256bit-plain-secret-0123456789-ABCDEFGHIJKLMNOPQRSTUVWXYZ",
    "spring.sql.init.mode=never",
    // 여기가 핵심
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
        "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration," +
        "org.springframework.boot.actuate.autoconfigure.data.redis.RedisReactiveHealthContributorAutoConfiguration",
    "management.health.redis.enabled=false"
})
@AutoConfigureTestDatabase
@DirtiesContext
class OrderFlowIntegrationTest {

	@Autowired private OrderService orderService;
	@Autowired private UserRepository userRepository;
	@Autowired private ItemRepository itemRepository;

	// Redis 관련 빈 mock 처리로 연결 시도 차단
	@MockBean private RedisConnectionFactory redisConnectionFactory;
	@MockBean private RedisMessageListenerContainer redisMessageListenerContainer;
	@MockBean private MessageListenerAdapter messageListenerAdapter;
	@MockBean(name = "chatPubSub") private StringRedisTemplate chatPubSubTemplate;
	@MockBean(name = "redisTemplate") private RedisTemplate<String, String> redisTemplate;
	@MockBean private RedisPubSubService redisPubSubService;

	@Test
	@Transactional
	@DisplayName("주문 등록 -> 조회 -> 취소 end-to-end")
	void e2e_order_register_query_cancel() {
		// given: 회원/상품 저장
		UserEntity member = UserEntity.builder()
				.username("user")
				.email("user@example.com")
				.address(new Address("city","street","zip"))
				.build();
		userRepository.save(member);

		Item item = new Item();
		item.setName("상품");
		item.setPrice(1000);
		item.setStockQuantity(10);
		itemRepository.save(item);

		// when: 주문 생성
		Long orderId = orderService.order(member.getId(), item.getId(), 3);

		// then: 주문 조회 및 검증
		Order order = orderService.findOrderWithMemberAndItems(orderId);
		assertThat(order.getMember().getEmail()).isEqualTo("user@example.com");
		assertThat(order.getOrderItems()).hasSize(1);
		OrderItem oi = order.getOrderItems().get(0);
		assertThat(oi.getCount()).isEqualTo(3);
		assertThat(oi.getOrderPrice()).isEqualTo(1000);
		assertThat(order.getTotalPrice()).isEqualTo(3000);

		// when: 주문 취소
		orderService.cancelOrder(orderId);

		// then: 재고 복구 및 상태 확인
		Item reloaded = itemRepository.findOne(item.getId());
		assertThat(reloaded.getStockQuantity()).isEqualTo(10);
	}

	@Test
	@Transactional
	@DisplayName("권한별 주문 조회: ADMIN은 전체, USER는 본인만")
	void find_by_role_admin_vs_user() {
		// seed data
		UserEntity admin = UserEntity.builder().username("admin").email("admin@example.com").build();
		UserEntity u1 = UserEntity.builder().username("u1").email("u1@example.com").build();
		UserEntity u2 = UserEntity.builder().username("u2").email("u2@example.com").build();
		userRepository.saveAll(java.util.List.of(admin, u1, u2));

		Item item = new Item(); item.setName("상품"); item.setPrice(500); item.setStockQuantity(100); itemRepository.save(item);

		// u1 주문 2건, u2 주문 1건 생성
		orderService.order(u1.getId(), item.getId(), 1);
		orderService.order(u1.getId(), item.getId(), 2);
		orderService.order(u2.getId(), item.getId(), 3);

		List<Order> all = orderService.findOrdersByRole(com.example.backend.security.entity.Role.ADMIN, "admin@example.com");
		assertThat(all.size()).isGreaterThanOrEqualTo(3);

		List<Order> onlyU1 = orderService.findOrdersByRole(com.example.backend.security.entity.Role.USER, "u1@example.com");
		assertThat(onlyU1).allMatch(o -> o.getMember().getEmail().equals("u1@example.com"));
	}
}
