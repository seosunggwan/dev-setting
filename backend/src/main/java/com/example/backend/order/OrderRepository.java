package com.example.backend.order;

import com.example.backend.item.QCategory;
import com.example.backend.item.domain.QItem;
import com.example.backend.item.domain.Item;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

import static com.example.backend.delivery.QDelivery.delivery;
import static com.example.backend.item.domain.QItem.item;
import static com.example.backend.order.QOrder.order;
import static com.example.backend.order.QOrderItem.orderItem;
import static com.example.backend.security.entity.QUserEntity.userEntity;

@Repository
public class OrderRepository {

    private final EntityManager em;
    private final JPAQueryFactory queryFactory;

    public OrderRepository(EntityManager em, JPAQueryFactory queryFactory) {
        this.em = em;
        this.queryFactory = queryFactory;
    }

    /* ========== C/U/R ========== */

    public void save(Order orderEntity) {
        em.persist(orderEntity);
    }

    public Order findOne(Long id) {
        return em.find(Order.class, id);
    }

    /**
     * 주문 단건 + 연관 모두(fetch) — 단건이라 컬렉션 fetch join 허용
     */
    public Order findOrderWithMemberAndItems(Long orderId) {
        return queryFactory
                .selectFrom(order)
                .distinct()
                .join(order.member, userEntity).fetchJoin()
                .join(order.delivery, delivery).fetchJoin()
                .leftJoin(order.orderItems, orderItem).fetchJoin()
                .leftJoin(orderItem.item, item).fetchJoin()
                .where(order.id.eq(orderId))
                .fetchOne();
    }

    /**
     * 주문 목록 (to-one만 fetch join) — 컬렉션은 배치로딩(@BatchSize or default_batch_fetch_size)로
     */
    public List<Order> findAllWithMemberAndItems() {
        return queryFactory
                .selectFrom(order)
                .join(order.member, userEntity).fetchJoin()
                .join(order.delivery, delivery).fetchJoin()
                .orderBy(order.orderDate.desc(), order.id.desc())
                .fetch();
    }

    public List<Order> findAll() {
        return queryFactory
                .selectFrom(order)
                .orderBy(order.id.desc())
                .fetch();
    }

    /* ========== 페이징 목록 (2쿼리 패턴) ========== */

    /**
     * 페이징: 1) ID만 페이지로 가져오고 2) 그 ID들 상세 로딩(필요시 컬렉션 fetch join)
     */
    public List<Order> findAllWithPaging(int offset, int limit) {
        // 1) 페이지 ID
        List<Long> ids = queryFactory
                .select(order.id)
                .from(order)
                .join(order.member, userEntity)
                .join(order.delivery, delivery)
                .orderBy(order.orderDate.desc(), order.id.desc())
                .offset(offset)
                .limit(limit)
                .fetch();

        if (ids.isEmpty()) return List.of();

        // 2) 상세 조회 (여기서만 컬렉션 fetch join 허용)
        return queryFactory
                .selectFrom(order).distinct()
                .join(order.member, userEntity).fetchJoin()
                .join(order.delivery, delivery).fetchJoin()
                .leftJoin(order.orderItems, orderItem).fetchJoin()
                .leftJoin(orderItem.item, item).fetchJoin()
                .where(order.id.in(ids))
                .orderBy(order.orderDate.desc(), order.id.desc())
                .fetch();
    }

    /**
     * 동적 검색 + 페이징 (to-one만 1차, 2차에서 컬렉션 fetch)
     */
    public List<Order> findAllByStringWithPaging(OrderSearch search, int offset, int limit) {
        BooleanBuilder where = baseFilters(search);

        // 1) 페이지 ID
        var idQuery = queryFactory
                .select(order.id)
                .from(order);

        if (StringUtils.hasText(search.getMemberName())) {
            idQuery.join(order.member, userEntity)
                   .where(userEntity.username.contains(search.getMemberName()));
        }
        idQuery.where(where)
               .orderBy(order.orderDate.desc(), order.id.desc())
               .offset(offset)
               .limit(limit);

        List<Long> ids = idQuery.fetch();
        if (ids.isEmpty()) return List.of();

        // 2) 상세
        var q = queryFactory
                .selectFrom(order).distinct()
                .join(order.member, userEntity).fetchJoin()
                .join(order.delivery, delivery).fetchJoin()
                .where(order.id.in(ids))
                .orderBy(order.orderDate.desc(), order.id.desc());

        // 결과에서 orderItems/item 접근한다면 컬렉션 fetch join 추가
        q.leftJoin(order.orderItems, orderItem).fetchJoin()
         .leftJoin(orderItem.item, item).fetchJoin();

        return q.fetch();
    }

    /* ========== 카운트 ========== */

    public long count() {
        Long cnt = queryFactory
                .select(order.id.count())
                .from(order)
                .fetchOne();
        return cnt == null ? 0L : cnt;
    }

    /**
     * 동적 검색 카운트 — 조인 최소화 (memberName 있을 때만 member 조인)
     */
    public long countBySearch(OrderSearch search) {
        BooleanBuilder where = baseFilters(search);

        var q = queryFactory
                .select(order.id.countDistinct())
                .from(order);

        if (StringUtils.hasText(search.getMemberName())) {
            q.join(order.member, userEntity)
             .where(userEntity.username.contains(search.getMemberName()));
        }

        Long cnt = q.where(where).fetchOne();
        return cnt == null ? 0L : cnt;
    }

    /* ========== JPQL findAllByString 대체 (비페이징) ========== */

    public List<Order> findAllByString(OrderSearch search) {
        BooleanBuilder where = baseFilters(search);

        var q = queryFactory
                .selectFrom(order)
                .join(order.member, userEntity).fetchJoin()
                .join(order.delivery, delivery).fetchJoin()
                .where(where)
                .orderBy(order.orderDate.desc(), order.id.desc());

        if (StringUtils.hasText(search.getMemberName())) {
            q.where(userEntity.username.contains(search.getMemberName()));
        }

        // 컬렉션은 배치 로딩에 맡김
        return q.fetch();
    }

    public List<Order> findAllByCriteria(OrderSearch search) {
        return findAllByString(search);
    }

    /* ========== 회원별 조회/카운트 (2쿼리 적용) ========== */

    public List<Order> findAllByMemberWithMemberAndItems(String memberEmail) {
        // to-one만 fetch
        return queryFactory
                .selectFrom(order)
                .join(order.member, userEntity).fetchJoin()
                .join(order.delivery, delivery).fetchJoin()
                .where(userEntity.email.eq(memberEmail))
                .orderBy(order.orderDate.desc(), order.id.desc())
                .fetch();
    }

    public List<Order> findAllByMemberWithPaging(String memberEmail, int offset, int limit) {
        // 1) ID 페이징
        List<Long> ids = queryFactory
                .select(order.id)
                .from(order)
                .join(order.member, userEntity)
                .where(userEntity.email.eq(memberEmail))
                .orderBy(order.orderDate.desc(), order.id.desc())
                .offset(offset)
                .limit(limit)
                .fetch();

        if (ids.isEmpty()) return List.of();

        // 2) 상세
        return queryFactory
                .selectFrom(order).distinct()
                .join(order.member, userEntity).fetchJoin()
                .join(order.delivery, delivery).fetchJoin()
                .leftJoin(order.orderItems, orderItem).fetchJoin()
                .leftJoin(orderItem.item, item).fetchJoin()
                .where(order.id.in(ids))
                .orderBy(order.orderDate.desc(), order.id.desc())
                .fetch();
    }

    public long countByMember(String memberEmail) {
        Long cnt = queryFactory
                .select(order.id.countDistinct())
                .from(order)
                .join(order.member, userEntity)
                .where(userEntity.email.eq(memberEmail))
                .fetchOne();
        return cnt == null ? 0L : cnt;
    }

    /**
     * 회원별 검색 (비페이징) — to-one fetch, 컬렉션은 배치로딩
     */
    public List<Order> findByMemberAndSearch(String memberEmail, OrderSearch search) {
        BooleanBuilder where = baseFilters(search)
                .and(userEntity.email.eq(memberEmail));

        return queryFactory
                .selectFrom(order)
                .join(order.member, userEntity).fetchJoin()
                .join(order.delivery, delivery).fetchJoin()
                .where(where)
                .orderBy(order.orderDate.desc(), order.id.desc())
                .fetch();
    }

    /**
     * 회원별 검색 + 페이징 (2쿼리)
     */
    public List<Order> findByMemberAndSearchWithPaging(String memberEmail, OrderSearch search, int offset, int limit) {
        BooleanBuilder where = baseFilters(search);

        // 1) ID 페이징
        var idQ = queryFactory
                .select(order.id)
                .from(order)
                .join(order.member, userEntity)
                .where(userEntity.email.eq(memberEmail))
                .where(where)
                .orderBy(order.orderDate.desc(), order.id.desc())
                .offset(offset)
                .limit(limit);

        List<Long> ids = idQ.fetch();
        if (ids.isEmpty()) return List.of();

        // 2) 상세
        return queryFactory
                .selectFrom(order).distinct()
                .join(order.member, userEntity).fetchJoin()
                .join(order.delivery, delivery).fetchJoin()
                .leftJoin(order.orderItems, orderItem).fetchJoin()
                .leftJoin(orderItem.item, item).fetchJoin()
                .where(order.id.in(ids))
                .orderBy(order.orderDate.desc(), order.id.desc())
                .fetch();
    }

    public long countByMemberAndSearch(String memberEmail, OrderSearch search) {
        BooleanBuilder where = baseFilters(search);

        Long cnt = queryFactory
                .select(order.id.countDistinct())
                .from(order)
                .join(order.member, userEntity)
                .where(userEntity.email.eq(memberEmail))
                .where(where)
                .fetchOne();
        return cnt == null ? 0L : cnt;
    }

    /* ========== 디버깅용 ========== */

    // Category 테이블의 이름 목록 조회
    public List<String> findAllCategories() {
        QCategory category = QCategory.category;
        return queryFactory
                .select(category.name)
                .from(category)
                .where(category.name.isNotNull())
                .distinct()
                .orderBy(category.name.asc())
                .fetch();
    }

    public List<Object[]> findCategoryStats() {
        QCategory category = QCategory.category;
        QItem qItem = QItem.item;

        return queryFactory
                .select(category.id, category.name, qItem.count())
                .from(category)
                .leftJoin(category.items, qItem)
                .groupBy(category.id, category.name)
                .orderBy(category.id.asc())
                .fetch()
                .stream()
                .map(t -> new Object[]{ t.get(category.id), t.get(category.name), t.get(qItem.count()) })
                .toList();
    }

    public List<Object[]> findItemCategoryInfo() {
        QItem qItem = QItem.item;
        QCategory category = QCategory.category;

        return queryFactory
                .select(qItem.id, qItem.name, category.name)
                .from(qItem)
                .leftJoin(qItem.categories, category)
                .orderBy(qItem.id.asc())
                .fetch()
                .stream()
                .map(t -> new Object[]{ t.get(qItem.id), t.get(qItem.name), t.get(category.name) })
                .toList();
    }

    /* ========== 내부 유틸 ========== */

    /**
     * 공통 검색 조건 (memberName 제외: 호출부에서 필요 시 userEntity 조인 + where 추가)
     * - 상태
     * - 아이템명 exists
     * - 카테고리(한글 카테고리명으로 직접 검색)
     * - 날짜 범위
     * - 금액 범위 (exists)
     */
    private BooleanBuilder baseFilters(OrderSearch s) {
        BooleanBuilder where = new BooleanBuilder();

        // 상태
        if (s.getOrderStatus() != null) {
            where.and(order.status.eq(s.getOrderStatus()));
        }

        // 아이템명 exists
        if (StringUtils.hasText(s.getItemName())) {
            where.and(JPAExpressions
                    .selectOne()
                    .from(orderItem)
                    .join(orderItem.item, item)
                    .where(
                            orderItem.order.eq(order),
                            item.name.contains(s.getItemName())
                    ).exists());
        }

        // 카테고리 (한글 카테고리명으로 직접 검색)
        if (StringUtils.hasText(s.getCategoryName())) {
            var i = QItem.item;
            var c = QCategory.category;
            where.and(JPAExpressions
                    .selectOne()
                    .from(orderItem)
                    .join(orderItem.item, i)
                    .join(i.categories, c)
                    .where(
                            orderItem.order.eq(order),
                            c.name.eq(s.getCategoryName())  // 한글 카테고리명으로 직접 비교
                    ).exists());
        }

        // 날짜 범위
        if (s.getOrderDateFrom() != null) {
            where.and(order.orderDate.goe(s.getOrderDateFrom()));
        }
        if (s.getOrderDateTo() != null) {
            where.and(order.orderDate.loe(s.getOrderDateTo()));
        }

        // 금액 범위 (주문항목 단위 합산 조건: exists)
        if (s.getMinPrice() != null) {
            where.and(JPAExpressions
                    .selectOne()
                    .from(orderItem)
                    .where(
                            orderItem.order.eq(order),
                            orderItem.orderPrice.multiply(orderItem.count).goe(s.getMinPrice())
                    ).exists());
        }
        if (s.getMaxPrice() != null) {
            where.and(JPAExpressions
                    .selectOne()
                    .from(orderItem)
                    .where(
                            orderItem.order.eq(order),
                            orderItem.orderPrice.multiply(orderItem.count).loe(s.getMaxPrice())
                    ).exists());
        }

        return where;
    }
}
