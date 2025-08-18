package com.example.backend.item;

import com.example.backend.item.domain.Item;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static com.example.backend.item.QCategory.category;

@Repository
public class CategoryRepository {

    private final EntityManager em;
    private final JPAQueryFactory queryFactory;

    public CategoryRepository(EntityManager em, JPAQueryFactory queryFactory) {
        this.em = em;
        this.queryFactory = queryFactory;
    }

    public void save(Category category) {
        if (category.getId() == null) {
            em.persist(category);
        } else {
            em.merge(category);
        }
    }

    public Optional<Category> findByName(String name) {
        Category result = queryFactory
                .selectFrom(category)
                .where(category.name.eq(name))
                .fetchOne();
        return Optional.ofNullable(result);
    }

    public List<Category> findAll() {
        return queryFactory
                .selectFrom(category)
                .fetch();
    }

    public Category findOrCreateByName(String name) {
        return findByName(name)
                .orElseGet(() -> {
                    Category newCategory = new Category();
                    newCategory.setName(name);
                    save(newCategory);
                    return newCategory;
                });
    }
}
