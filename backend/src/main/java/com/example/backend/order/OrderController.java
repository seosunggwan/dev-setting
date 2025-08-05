package com.example.backend.order;

import com.example.backend.item.ItemService;
import com.example.backend.item.domain.Item;
import com.example.backend.order.dto.OrderFormData;
import com.example.backend.order.dto.OrderRequest;
import com.example.backend.order.dto.OrderResponseDto;
import com.example.backend.order.dto.PagedOrdersDto;
import com.example.backend.security.entity.Role;
import com.example.backend.security.entity.UserEntity;
import com.example.backend.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/orders")          // ⬅️ 공통 prefix
@Slf4j
public class OrderController {

    private final OrderService orderService;
    private final UserRepository userRepository;
    private final ItemService itemService;

    /**
     * 현재 인증된 사용자의 이메일 추출
     */
    private String getCurrentUserEmail(Authentication authentication) {
        return authentication.getName();
    }

    /**
     * 현재 인증된 사용자의 역할 추출
     */
    private Role getCurrentUserRole(Authentication authentication) {
        String authority = authentication.getAuthorities().iterator().next().getAuthority();
        // "ROLE_" 접두사 제거
        String roleName = authority.replace("ROLE_", "");
        return Role.valueOf(roleName);
    }

    /** 주문 폼용 데이터 (역할 기반) */
    @GetMapping("/form")
    @Transactional(readOnly = true)
    public ResponseEntity<OrderFormData> createForm(Authentication authentication) {
        String userEmail = getCurrentUserEmail(authentication);
        Role userRole = getCurrentUserRole(authentication);
        
        log.info("주문 폼 데이터 요청 - 사용자: {}, 역할: {}", userEmail, userRole);
        
        List<UserEntity> members;
        if (userRole == Role.ADMIN) {
            // 관리자는 모든 회원 조회 가능
            members = Optional.ofNullable(userRepository.findAll()).orElse(Collections.emptyList());
        } else {
            // 일반 사용자는 본인만 조회
            UserEntity currentUser = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new IllegalArgumentException("현재 사용자를 찾을 수 없습니다: " + userEmail));
            members = Collections.singletonList(currentUser);
        }
        
        List<Item> items = Optional.ofNullable(itemService.findItems()).orElse(Collections.emptyList());

        OrderFormData formData = new OrderFormData(
            members.stream()
                .map(m -> new OrderFormData.MemberDto(m.getId(), m.getUsername()))
                .collect(Collectors.toList()),
            items.stream()
                .map(i -> new OrderFormData.ItemDto(i.getId(), i.getName(), i.getPrice(), i.getStockQuantity()))
                .collect(Collectors.toList())
        );

        log.info("조회된 회원 수: {}, 상품 수: {}", members.size(), items.size());
        return ResponseEntity.ok(formData);
    }

    @PostMapping
    @Transactional
    public ResponseEntity<OrderResponseDto> order(@RequestBody OrderRequest request) {
        Long orderId = orderService.order(request.getMemberId(), request.getItemId(), request.getCount());
        Order order = orderService.findOrderWithMemberAndItems(orderId);
        return ResponseEntity.ok(new OrderResponseDto(order));
    }

    @GetMapping("/{orderId}")
    @Transactional(readOnly = true)
    public ResponseEntity<OrderResponseDto> getOrder(@PathVariable Long orderId) {
        Order order = orderService.findOrderWithMemberAndItems(orderId);
        return ResponseEntity.ok(new OrderResponseDto(order));
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<OrderResponseDto>> getOrders(Authentication authentication) {
        String userEmail = getCurrentUserEmail(authentication);
        Role userRole = getCurrentUserRole(authentication);
        
        log.info("주문 목록 조회 요청 - 사용자: {}, 역할: {}", userEmail, userRole);
        
        List<Order> orders = orderService.findOrdersByRole(userRole, userEmail);
        List<OrderResponseDto> result = orders.stream()
                .map(OrderResponseDto::new)
                .collect(Collectors.toList());
                
        log.info("조회된 주문 수: {}", result.size());
        return ResponseEntity.ok(result);
    }

    /**
     * 주문 검색 API (역할 기반)
     * - ADMIN: 모든 주문에서 회원 이름, 주문 상태로 검색
     * - USER: 본인 주문에서 주문 상태로만 검색 (회원 이름 검색 무시)
     */
    @GetMapping("/search")
    @Transactional(readOnly = true)
    public ResponseEntity<List<OrderResponseDto>> searchOrders(
            @RequestParam(required = false) String memberName,
            @RequestParam(required = false) OrderStatus orderStatus,
            Authentication authentication) {

        String userEmail = getCurrentUserEmail(authentication);
        Role userRole = getCurrentUserRole(authentication);
        
        log.info("주문 검색 요청 - 사용자: {}, 역할: {}, 회원명: {}, 주문상태: {}", 
                userEmail, userRole, memberName, orderStatus);

        // 검색 조건 객체 생성
        OrderSearch orderSearch = new OrderSearch();
        if (userRole == Role.ADMIN) {
            // 관리자는 회원 이름으로도 검색 가능
            orderSearch.setMemberName(memberName);
        }
        // 일반 사용자는 회원 이름 검색 무시
        orderSearch.setOrderStatus(orderStatus);

        // 역할별 검색 실행
        List<Order> orders = orderService.findOrdersByRoleAndSearch(userRole, userEmail, orderSearch);

        // 응답 DTO 변환
        List<OrderResponseDto> result = orders.stream()
                .map(OrderResponseDto::new)
                .collect(Collectors.toList());

        log.info("검색 결과 수: {}", result.size());
        return ResponseEntity.ok(result);
    }

    /**
     * 페이지네이션이 적용된 주문 목록 조회 API
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지당 항목 수
     * @return 페이지네이션이 적용된 주문 목록과 페이지 정보
     */
    @GetMapping("/page")
    @Transactional(readOnly = true)
    public ResponseEntity<PagedOrdersDto> getOrdersWithPaging(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        String userEmail = getCurrentUserEmail(authentication);
        Role userRole = getCurrentUserRole(authentication);
        
        log.info("페이지네이션 주문 목록 조회 요청 - 사용자: {}, 역할: {}, 페이지: {}, 사이즈: {}", 
                userEmail, userRole, page, size);

        // 페이지 크기 제한
        if (size > 50) {
            size = 50;
        }

        // 역할별 서비스 호출
        PagedOrdersDto result = orderService.findOrdersByRoleWithPaging(userRole, userEmail, page, size);

        log.info("페이지 {}의 주문 {}개 조회 완료 (전체 {}개)", page, result.getOrders().size(), result.getPageInfo().getTotal());
        return ResponseEntity.ok(result);
    }

    /**
     * 페이지네이션이 적용된 주문 검색 API (역할 기반)
     * @param memberName 회원 이름 검색어 (ADMIN만 사용 가능)
     * @param orderStatus 주문 상태
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지당 항목 수
     * @return 페이지네이션이 적용된 주문 검색 결과와 페이지 정보
     */
    @GetMapping("/search/page")
    @Transactional(readOnly = true)
    public ResponseEntity<PagedOrdersDto> searchOrdersWithPaging(
            @RequestParam(required = false) String memberName,
            @RequestParam(required = false) OrderStatus orderStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        String userEmail = getCurrentUserEmail(authentication);
        Role userRole = getCurrentUserRole(authentication);
        
        log.info("페이지네이션 주문 검색 요청 - 사용자: {}, 역할: {}, 회원명: {}, 주문상태: {}, 페이지: {}, 사이즈: {}",
                userEmail, userRole, memberName, orderStatus, page, size);

        // 페이지 크기 제한
        if (size > 50) {
            size = 50;
        }

        // 검색 조건 객체 생성
        OrderSearch orderSearch = new OrderSearch();
        if (userRole == Role.ADMIN) {
            // 관리자는 회원 이름으로도 검색 가능
            orderSearch.setMemberName(memberName);
        }
        // 일반 사용자는 회원 이름 검색 무시
        orderSearch.setOrderStatus(orderStatus);

        // 역할별 서비스 호출
        PagedOrdersDto result = orderService.findOrdersByRoleAndSearchWithPaging(
                userRole, userEmail, orderSearch, page, size);

        log.info("페이지 {}의 주문 {}개 검색 완료 (전체 {}개)", page, result.getOrders().size(), result.getPageInfo().getTotal());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{orderId}/cancel")
    @Transactional
    public ResponseEntity<OrderResponseDto> cancelOrder(@PathVariable Long orderId) {
        orderService.cancelOrder(orderId);
        Order order = orderService.findOrderWithMemberAndItems(orderId);
        return ResponseEntity.ok(new OrderResponseDto(order));
    }
}
