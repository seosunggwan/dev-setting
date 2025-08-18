package com.example.backend.item;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static com.example.backend.item.QCategory.category;

@Repository
@Transactional(readOnly = true)
public class CategoryRepository {

    private final EntityManager em;
    private final JPAQueryFactory queryFactory;

    public CategoryRepository(EntityManager em, JPAQueryFactory queryFactory) {
        this.em = em;
        this.queryFactory = queryFactory;
    }

    /* ================== C / U ================== */

    @Transactional
    public void save(Category entity) {
        if (entity.getId() == null) {
            em.persist(entity);
        } else {
            // 권장: 서비스에서 find 후 필드만 변경(변경감지). 필요 시에만 merge 사용.
            em.merge(entity);
        }
    }

    /* ================ R (조회) ================= */

    public Optional<Category> findByName(String name) {
        if (name == null) return Optional.empty();
        String key = normalize(name);

        Category result = queryFactory
                .selectFrom(category)
                .where(category.name.eq(key))
                .fetchFirst(); // 중복 방지 못해도 첫 행 반환

        return Optional.ofNullable(result);
    }

    public List<Category> findAll() {
        return queryFactory
                .selectFrom(category)
                .orderBy(category.id.asc())
                .fetch();
    }

    /**
     * 동시성 안전한 find-or-create
     * - name 컬럼에 UNIQUE 제약이 걸려 있어야 함(아래 참고)
     * - race 시 INSERT가 한 번만 성공, 나머지는 예외 -> 재조회로 수렴
     */
    @Transactional
    public Category findOrCreateByName(String name) {
        String key = normalize(name);

        // 1) 빠른 길: 먼저 조회
        Category found = queryFactory
                .selectFrom(category)
                .where(category.name.eq(key))
                .fetchFirst();
        if (found != null) return found;

        // 2) 없으면 생성 시도
        Category created = new Category();
        created.setName(key);

        try {
            em.persist(created);
            // flush 해서 UNIQUE 위반을 즉시 감지(트랜잭션 말미에 한 번에 터지는 것 방지)
            em.flush();
            return created;
        } catch (PersistenceException dup) {
            // 동시 INSERT 경합으로 UNIQUE 위반 발생한 케이스
            em.clear(); // 직전 persist된 엔티티 분리(선택)
            Category fallback = queryFactory
                    .selectFrom(category)
                    .where(category.name.eq(key))
                    .fetchFirst();
            if (fallback != null) return fallback;
            throw dup; // 진짜 다른 오류면 그대로 전파
        }
    }

    /* ================ 내부 유틸 ================= */

    /** 공백 트리밍 등 입력 정규화 (코드값 쓴다면 toUpperCase 등 추가) */
    private String normalize(String s) {
        return s == null ? null : s.trim();
    }
}
