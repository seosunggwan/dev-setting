package com.example.backend.order;

import com.example.backend.item.domain.Item;
import com.example.backend.item.QCategory;
import com.example.backend.item.domain.QItem;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

import static com.example.backend.order.QOrder.order;
import static com.example.backend.order.QOrderItem.orderItem;
import static com.example.backend.item.domain.QItem.item;
import static com.example.backend.security.entity.QUserEntity.userEntity;
import static com.example.backend.delivery.QDelivery.delivery;

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
     * 주문 목록 (to-one만 fetch join) + 배치로딩 트리거
     */
    public List<Order> findAllWithMemberAndItems() {
        List<Order> orders = queryFactory
                .selectFrom(order)
                .join(order.member, userEntity).fetchJoin()
                .join(order.delivery, delivery).fetchJoin()
                .leftJoin(order.orderItems, orderItem).fetchJoin()
                .leftJoin(orderItem.item, item).fetchJoin()  // Item도 fetchJoin
                .orderBy(order.orderDate.desc())
                .fetch();

        return orders;
    }

    public List<Order> findAll() {
        return queryFactory.selectFrom(order).fetch();
    }

    /* ========== 페이징 목록 ========== */

    /**
     * 페이지네이션: to-one만 fetch join
     */
    public List<Order> findAllWithPaging(int offset, int limit) {
        return queryFactory
                .selectFrom(order)
                .join(order.member, userEntity).fetchJoin()
                .join(order.delivery, delivery).fetchJoin()
                .leftJoin(order.orderItems, orderItem).fetchJoin()
                .leftJoin(orderItem.item, item).fetchJoin()  // Item도 fetchJoin
                .orderBy(order.orderDate.desc())
                .offset(offset)
                .limit(limit)
                .fetch();
    }

    /**
     * 동적 검색 + 페이징 (to-one fetch join, 나머지 exists 서브쿼리)
     */
    public List<Order> findAllByStringWithPaging(OrderSearch search, int offset, int limit) {
        BooleanBuilder where = buildCommonFilters(search);

        return queryFactory
                .selectFrom(order)
                .join(order.member, userEntity).fetchJoin()
                .join(order.delivery, delivery).fetchJoin()
                .leftJoin(order.orderItems, orderItem).fetchJoin()
                .leftJoin(orderItem.item, item).fetchJoin()  // Item도 fetchJoin
                .where(where)
                .orderBy(order.orderDate.desc())
                .offset(offset)
                .limit(limit)
                .fetch();
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
     * 동적 검색 카운트 (join 최소화: member만 조인, 나머지는 exists)
     */
    public long countBySearch(OrderSearch search) {
        BooleanBuilder where = buildCommonFilters(search);

        Long cnt = queryFactory
                .select(order.id.countDistinct())
                .from(order)
                .join(order.member, userEntity) // 회원명 조건 가능성 때문에 조인
                .where(where)
                .fetchOne();
        return cnt == null ? 0L : cnt;
    }

    /* ========== JPQL findAllByString 대체 (비페이징) ========== */

    public List<Order> findAllByString(OrderSearch search) {
        BooleanBuilder where = buildCommonFilters(search);

        return queryFactory
                .selectFrom(order)
                .join(order.member, userEntity).fetchJoin()
                .join(order.delivery, delivery).fetchJoin()
                .where(where)
                .orderBy(order.orderDate.desc())
                .fetch();
    }

    public List<Order> findAllByCriteria(OrderSearch search) {
        // 유지: 내부적으로 QueryDSL 버전 사용하는 것으로 대체
        return findAllByString(search);
    }

    /* ========== 회원별 조회/카운트 ========== */

    public List<Order> findAllByMemberWithMemberAndItems(String memberEmail) {
        List<Order> orders = queryFactory
                .selectFrom(order)
                .join(order.member, userEntity).fetchJoin()
                .join(order.delivery, delivery).fetchJoin()
                .leftJoin(order.orderItems, orderItem).fetchJoin()
                .leftJoin(orderItem.item, item).fetchJoin()  // Item도 fetchJoin
                .where(userEntity.email.eq(memberEmail))
                .orderBy(order.orderDate.desc())
                .fetch();

        return orders;
    }

    public List<Order> findAllByMemberWithPaging(String memberEmail, int offset, int limit) {
        return queryFactory
                .selectFrom(order)
                .join(order.member, userEntity).fetchJoin()
                .join(order.delivery, delivery).fetchJoin()
                .leftJoin(order.orderItems, orderItem).fetchJoin()
                .leftJoin(orderItem.item, item).fetchJoin()  // Item도 fetchJoin
                .where(userEntity.email.eq(memberEmail))
                .orderBy(order.orderDate.desc())
                .offset(offset)
                .limit(limit)
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
     * 회원별 검색 (비페이징)
     */
    public List<Order> findByMemberAndSearch(String memberEmail, OrderSearch search) {
        BooleanBuilder where = buildCommonFilters(search)
                .and(userEntity.email.eq(memberEmail));

        List<Order> result = queryFactory
                .selectFrom(order)
                .join(order.member, userEntity).fetchJoin()
                .join(order.delivery, delivery).fetchJoin()
                .leftJoin(order.orderItems, orderItem).fetchJoin()
                .leftJoin(orderItem.item, item).fetchJoin()  // Item도 fetchJoin
                .where(where)
                .orderBy(order.orderDate.desc())
                .fetch();

        return result;
    }

    /**
     * 회원별 검색 + 페이징
     */
    public List<Order> findByMemberAndSearchWithPaging(String memberEmail, OrderSearch search, int offset, int limit) {
        BooleanBuilder where = buildCommonFilters(search)
                .and(userEntity.email.eq(memberEmail));

        return queryFactory
                .selectFrom(order)
                .join(order.member, userEntity).fetchJoin()
                .join(order.delivery, delivery).fetchJoin()
                .leftJoin(order.orderItems, orderItem).fetchJoin()
                .leftJoin(orderItem.item, item).fetchJoin()  // Item도 fetchJoin
                .where(where)
                .orderBy(order.orderDate.desc())
                .offset(offset)
                .limit(limit)
                .fetch();
    }

    public long countByMemberAndSearch(String memberEmail, OrderSearch search) {
        BooleanBuilder where = buildCommonFilters(search)
                .and(userEntity.email.eq(memberEmail));

        Long cnt = queryFactory
                .select(order.id.countDistinct())
                .from(order)
                .join(order.member, userEntity)
                .where(where)
                .fetchOne();
        return cnt == null ? 0L : cnt;
    }

    /* ========== 디버깅용 카테고리 ========== */

    // Hibernate 고유의 dtype 컬럼 대신, Category 테이블에서 이름 목록 조회
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

    /**
     * 카테고리별 상품 수 조회 (디버깅용)
     */
    public List<Object[]> findCategoryStats() {
        QCategory category = QCategory.category;
        QItem item = QItem.item;
        
        return queryFactory
                .select(
                    category.id,
                    category.name,
                    item.count()
                )
                .from(category)
                .leftJoin(category.items, item)
                .groupBy(category.id, category.name)
                .orderBy(category.id.asc())
                .fetch()
                .stream()
                .map(tuple -> new Object[]{
                    tuple.get(category.id),
                    tuple.get(category.name),
                    tuple.get(item.count())
                })
                .toList();
    }

    /**
     * 상품별 카테고리 정보 조회 (디버깅용)
     */
    public List<Object[]> findItemCategoryInfo() {
        QItem item = QItem.item;
        QCategory category = QCategory.category;
        
        return queryFactory
                .select(
                    item.id,
                    item.name,
                    category.name
                )
                .from(item)
                .leftJoin(item.categories, category)
                .orderBy(item.id.asc())
                .fetch()
                .stream()
                .map(tuple -> new Object[]{
                    tuple.get(item.id),
                    tuple.get(item.name),
                    tuple.get(category.name)
                })
                .toList();
    }

    /* ========== 내부 유틸 ========== */

    /**
     * 공통 검색 조건 조립
     */
    private BooleanBuilder buildCommonFilters(OrderSearch s) {
        BooleanBuilder where = new BooleanBuilder();

        // 상태
        if (s.getOrderStatus() != null) {
            where.and(order.status.eq(s.getOrderStatus()));
        }

        // 회원명
        if (StringUtils.hasText(s.getMemberName())) {
            where.and(userEntity.username.contains(s.getMemberName()));
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

        if (StringUtils.hasText(s.getCategoryName())) {
            // 한글 카테고리명으로 직접 검색
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
