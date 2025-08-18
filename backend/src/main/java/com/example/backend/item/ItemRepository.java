package com.example.backend.item;

import com.example.backend.item.domain.Item;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.example.backend.item.domain.QItem.item;
import static com.example.backend.order.QOrderItem.orderItem;

@Repository
@RequiredArgsConstructor
public class ItemRepository {

    private final EntityManager em;
    private final JPAQueryFactory queryFactory;

    /* ===================== C / U / R ===================== */

    public void save(Item itemEntity) {
        if (itemEntity.getId() == null) {
            em.persist(itemEntity);
        } else {
            // 권장은 변경감지(서비스에서 find 후 setter), 기존 유지면 merge
            em.merge(itemEntity);
        }
    }

    /** 1차 캐시 활용 + 프록시 불필요 시 em.find가 유리 */
    public Item findOne(Long id) {
        return em.find(Item.class, id);
    }

    /** 대량이면 지양(테스트/관리용), 읽기전용 힌트 */
    public List<Item> findAll() {
        return queryFactory
                .selectFrom(item)
                .orderBy(item.id.desc())
                .fetch();
    }

    /**
     * 페이지네이션 목록 (offset/limit)
     * * offset이 매우 커지면 키셋 페이징 메서드도 같이 쓰는 걸 추천(아래 참조)
     */
    public List<Item> findAllWithPaging(int offset, int limit) {
        return queryFactory
                .selectFrom(item)
                .orderBy(item.id.desc())
                .offset(offset)
                .limit(limit)
                .fetch();
    }

    /**
     * 이름 포함 검색 + 페이징
     * - MySQL 기본 collation이 대소문자 무시라면 containsIgnoreCase로 LOWER() 감싸 인덱스 깨지게 하지 말고 like 사용!
     * - 만약 DB가 대소문자 구분이면(또는 collation 다르면) containsIgnoreCase 유지하거나, 별도 정규화 컬럼/풀텍스트 고려
     */
    public List<Item> findByNameContaining(String keyword, int offset, int limit) {
        if (keyword == null || keyword.isBlank()) {
            return findAllWithPaging(offset, limit);
        }
        return queryFactory
                .selectFrom(item)
                .where(item.name.like("%" + keyword + "%"))
                .orderBy(item.id.desc())
                .offset(offset)
                .limit(limit)
                .fetch();
    }

    public long countByNameContaining(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return count();
        }
        Long cnt = queryFactory
                .select(item.id.count())
                .from(item)
                .where(item.name.like("%" + keyword + "%"))
                .fetchOne();
        return cnt != null ? cnt : 0L;
    }

    public long count() {
        Long cnt = queryFactory
                .select(item.id.count())
                .from(item)
                .fetchOne();
        return cnt != null ? cnt : 0L;
    }

    /* ===================== 삭제 ===================== */

    @Transactional
    public void delete(Long id) {
        // 0) 존재 여부 (select 1) — getReference 사용 전 안전 체크
        boolean exists = queryFactory
                .selectOne()
                .from(item)
                .where(item.id.eq(id))
                .fetchFirst() != null;
        if (!exists) {
            throw new IllegalStateException("삭제할 상품을 찾을 수 없습니다.");
        }

        // 1) 참조 여부 — COUNT 대신 EXISTS로 숏서킷
        boolean referenced = queryFactory
                .selectOne()
                .from(orderItem)
                .where(orderItem.item.id.eq(id))
                .fetchFirst() != null;

        if (referenced) {
            throw new IllegalStateException("이미 주문에 사용된 상품은 삭제할 수 없습니다.");
        }

        // 2) 실제 삭제 — 프록시로 불필요한 select 피함
        try {
            Item ref = em.getReference(Item.class, id);
            em.remove(ref);
        } catch (EntityNotFoundException e) {
            // 동시성 등으로 사라졌을 가능성
            throw new IllegalStateException("이미 삭제된 상품입니다.", e);
        }

        // ※ 완전 벌크가 필요하면 아래(영속성 컨텍스트 주의)
        // queryFactory.delete(item).where(item.id.eq(id)).execute();
    }

    /* ===================== 키셋(Seek) 페이징(옵션) ===================== */

    /**
     * 무한 스크롤/다음 페이지 같은 UX에 유리. 큰 offset 성능 저하 회피.
     * lastId가 null이면 첫 페이지.
     */
    public List<Item> findPageBySeek(Long lastId, int size) {
        return queryFactory
                .selectFrom(item)
                .where(lastId == null ? null : item.id.lt(lastId))
                .orderBy(item.id.desc())
                .limit(size)
                .fetch();
    }
}