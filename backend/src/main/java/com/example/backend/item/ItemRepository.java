package com.example.backend.item;

import com.example.backend.item.domain.Item;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
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

    public void save(Item itemEntity) {
        if (itemEntity.getId() == null) {
            em.persist(itemEntity);
        } else {
            // 변경감지로 업데이트하려면 find 후 필드만 변경하는 게 더 안전하지만,
            // 기존 로직 유지: merge 사용
            em.merge(itemEntity);
        }
    }

    public Item findOne(Long id) {
        return queryFactory
                .selectFrom(item)
                .where(item.id.eq(id))
                .fetchOne();
    }

    public List<Item> findAll() {
        return queryFactory
                .selectFrom(item)
                .fetch();
    }

    /**
     * 페이지네이션을 적용한 상품 목록 조회
     * @param offset 시작 위치
     * @param limit 가져올 데이터 수
     * @return 상품 목록
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
     * 검색어로 상품 목록 조회
     * @param keyword 검색어
     * @param offset 시작 위치
     * @param limit 가져올 데이터 수
     * @return 검색된 상품 목록
     */
    public List<Item> findByNameContaining(String keyword, int offset, int limit) {
        return queryFactory
                .selectFrom(item)
                .where(item.name.containsIgnoreCase(keyword))
                .orderBy(item.id.desc())
                .offset(offset)
                .limit(limit)
                .fetch();
    }

    /**
     * 검색어에 해당하는 상품 수 조회
     * @param keyword 검색어
     * @return 검색된 상품 수
     */
    public long countByNameContaining(String keyword) {
        Long cnt = queryFactory
                .select(item.count())
                .from(item)
                .where(item.name.containsIgnoreCase(keyword))
                .fetchOne();
        return cnt != null ? cnt : 0L;
    }

    /**
     * 전체 상품 수 조회
     * @return 전체 상품 수
     */
    public long count() {
        Long cnt = queryFactory
                .select(item.count())
                .from(item)
                .fetchOne();
        return cnt != null ? cnt : 0L;
    }

    @Transactional
    public void delete(Long id) {
        // 존재 확인 (영속 상태 보장 위해 em.find 사용)
        Item target = em.find(Item.class, id);
        if (target == null) {
            throw new IllegalStateException("삭제할 상품을 찾을 수 없습니다.");
        }

        // OrderItem 참조 여부 체크 (QueryDSL)
        Long refCnt = queryFactory
                .select(orderItem.count())
                .from(orderItem)
                .where(orderItem.item.id.eq(id))
                .fetchOne();

        if (refCnt != null && refCnt > 0) {
            throw new IllegalStateException("이미 주문에 사용된 상품은 삭제할 수 없습니다.");
        }

        em.remove(target);

        // ※ 대안) 라이프사이클 콜백/캐스케이드 필요 없고 벌크로 지워도 된다면:
        // queryFactory.delete(item).where(item.id.eq(id)).execute();
    }
}