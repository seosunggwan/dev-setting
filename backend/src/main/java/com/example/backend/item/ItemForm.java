package com.example.backend.item;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ItemForm {

    private Long id;
    private String name;
    private int price;
    private int stockQuantity;
    private String imageUrl;
    
    // 아이템 타입 (BOOK, ALBUM, MOVIE)
    private String itemType;

    // Book 필드
    private String author;
    private String isbn;
    
    // Album 필드
    private String artist;
    private String etc;
    
    // Movie 필드
    private String director;
    private String actor;
}
