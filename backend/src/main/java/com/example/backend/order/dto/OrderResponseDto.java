package com.example.backend.order.dto;

import com.example.backend.delivery.DeliveryStatus;
import com.example.backend.item.domain.Album;
import com.example.backend.item.domain.Book;
import com.example.backend.item.domain.Item;
import com.example.backend.item.domain.Movie;
import com.example.backend.order.Order;
import com.example.backend.order.OrderStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
public class OrderResponseDto {
    private Long orderId;
    private String memberName;
    private String memberEmail;
    private OrderStatus orderStatus;
    private LocalDateTime orderDate;
    private List<OrderItemDto> orderItems;
    private DeliveryStatus deliveryStatus;

    public OrderResponseDto(Order order) {
        this.orderId = order.getId();
        this.memberName = order.getMember().getUsername();
        this.memberEmail = order.getMember().getEmail();
        this.orderStatus = order.getStatus();
        this.orderDate = order.getOrderDate();
        this.orderItems = order.getOrderItems().stream()
                .map(OrderItemDto::new)
                .collect(Collectors.toList());
        this.deliveryStatus = order.getDelivery().getStatus();
    }

    @Getter
    @NoArgsConstructor
    public static class OrderItemDto {
        private String itemName;
        private String itemType;
        private String itemTypeDisplay;
        private int orderPrice;
        private int count;

        public OrderItemDto(com.example.backend.order.OrderItem orderItem) {
            Item item = orderItem.getItem();
            this.itemName = item.getName();
            this.orderPrice = orderItem.getOrderPrice();
            this.count = orderItem.getCount();
            
            // 상품 타입 판별
            if (item instanceof Book) {
                this.itemType = "BOOK";
                this.itemTypeDisplay = "도서";
            } else if (item instanceof Album) {
                this.itemType = "ALBUM";
                this.itemTypeDisplay = "음반";
            } else if (item instanceof Movie) {
                this.itemType = "MOVIE";
                this.itemTypeDisplay = "영화";
            } else {
                this.itemType = "UNKNOWN";
                this.itemTypeDisplay = "기타";
            }
        }
    }
} 