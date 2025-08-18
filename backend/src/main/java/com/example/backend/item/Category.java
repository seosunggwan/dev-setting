package com.example.backend.item;

import com.example.backend.item.domain.Item;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(
    name = "category",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_category_name", columnNames = "name")
    }
)
@Getter @Setter
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Long id;

    // 유니크 대상 컬럼
    @Column(name = "name", nullable = false, length = 50)
    @org.hibernate.annotations.NaturalId(mutable = false) // 선택사항(하이버네이트)
    private String name;

    @ManyToMany
    @JoinTable(name = "category_item",
            joinColumns = @JoinColumn(name = "category_id"),
            inverseJoinColumns = @JoinColumn(name = "item_id"))
    private List<Item> items = new ArrayList<>();

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @OneToMany(mappedBy = "parent")
    private List<Category> child = new ArrayList<>();

    //==연관관계 메서드==//
    public void addChildCategory(Category child) {
        this.child.add(child);
        child.setParent(this);
    }

    /** 아이템 추가 */
    public void addItem(Item item) {
        this.items.add(item);
        item.getCategories().add(this);
    }

    /** 아이템 제거 */
    public void removeItem(Item item) {
        this.items.remove(item);
        item.getCategories().remove(this);
    }

    /** 입력값 정규화 (선택) */
    @PrePersist @PreUpdate
    private void normalize() {
        if (this.name != null) {
            this.name = this.name.trim();
            // 코드값(예: "A","B","M")만 쓰면 아래도 가능
            // this.name = this.name.trim().toUpperCase();
        }
    }
}
