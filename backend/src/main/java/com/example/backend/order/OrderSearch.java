package com.example.backend.order;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter
public class OrderSearch {

    private String memberName; //회원 이름
    private OrderStatus orderStatus; //주문 상태[ORDER, CANCEL]
    
    // 추가된 검색 조건들
    private String itemName; // 상품명 검색
    private String categoryName; // 카테고리명 (음반, 도서, 영화 등)
    private LocalDateTime orderDateFrom; // 주문일시 시작
    private LocalDateTime orderDateTo; // 주문일시 끝
    private Integer minPrice; // 최소 금액
    private Integer maxPrice; // 최대 금액
}
