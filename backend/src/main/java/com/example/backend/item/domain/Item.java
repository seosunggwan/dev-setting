package com.example.backend.item.domain;

import com.example.backend.common.exception.NotEnoughStockException;
import com.example.backend.item.Category;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.List;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "dtype")
@DiscriminatorValue("Item")
@Getter @Setter
@BatchSize(size = 100)   // ✅ 여기에 붙이면 OrderItem → Item 로딩 시 배치 조회
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)   // DB의 AUTO_INCREMENT와 매핑
    @Column(name = "item_id")
    private Long id;

    private String name;
    private int price;
    private int stockQuantity;
    
    @Column(length = 512)
    private String imageUrl; // 상품 이미지 URL

    @ManyToMany(mappedBy = "items")
    private List<Category> categories = new ArrayList<>();

    //==비즈니스 로직==//
    /**
     * stock 증가
     */
    public void addStock(int quantity) {
        this.stockQuantity += quantity;
    }

    /**
     * stock 감소
     */
    public void removeStock(int quantity) {
        int restStock = this.stockQuantity - quantity;
        if (restStock < 0) {
            throw new NotEnoughStockException("need more stock");
        }
        this.stockQuantity = restStock;
    }

    /**
     * 카테고리 추가
     */
    public void addCategory(Category category) {
        this.categories.add(category);
        category.getItems().add(this);
    }

    /**
     * 아이템 타입에 따른 기본 카테고리명 반환
     */
    public String getDefaultCategoryName() {
        String dtype = this.getClass().getSimpleName();
        switch (dtype) {
            case "Book": return "도서";
            case "Album": return "음반";
            case "Movie": return "영화";
            default: return null;
        }
    }
}
