package com.example.backend.order;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;

@Repository
public class OrderRepository {

    private final EntityManager em;

    public OrderRepository(EntityManager em) {
        this.em = em;
    }

    public void save(Order order) {
        em.persist(order);
    }

    public Order findOne(Long id) {
        return em.find(Order.class, id);
    }

    /**
     * 주문 조회 (회원과 주문 상품 정보를 함께 조회)
     */
    public Order findOrderWithMemberAndItems(Long orderId) {
        return em.createQuery(
                "select o from Order o " +
                        "join fetch o.member m " +
                        "join fetch o.delivery d " +
                        "join fetch o.orderItems oi " +
                        "join fetch oi.item i " +
                        "where o.id = :orderId", Order.class)
                .setParameter("orderId", orderId)
                .getSingleResult();
    }

    /**
     * 주문 목록 조회 (회원과 주문 상품 정보를 함께 조회)
     */
    public List<Order> findAllWithMemberAndItems() {
        return em.createQuery(
                "select distinct o from Order o " +
                        "join fetch o.member m " +
                        "join fetch o.delivery d " +
                        "join fetch o.orderItems oi " +
                        "join fetch oi.item i", Order.class)
                .getResultList();
    }

    public List<Order> findAll() {
        return em.createQuery("select o from Order o", Order.class)
                .getResultList();
    }

    /**
     * 페이지네이션을 적용한 주문 목록 조회
     * @param offset 시작 위치
     * @param limit 가져올 데이터 수
     * @return 주문 목록
     */
    public List<Order> findAllWithPaging(int offset, int limit) {
        return em.createQuery(
            "select distinct o from Order o " +
            "join fetch o.member m " +
            "join fetch o.delivery d " +
            "join fetch o.orderItems oi " +
            "join fetch oi.item i " +
            "order by o.orderDate desc", Order.class)
            .setFirstResult(offset)
            .setMaxResults(limit)
            .getResultList();
    }

    /**
     * 페이지네이션을 적용한 주문 검색 - 확장된 검색 조건 지원
     * @param orderSearch 검색 조건
     * @param offset 시작 위치
     * @param limit 가져올 데이터 수
     * @return 검색 결과 주문 목록
     */
    public List<Order> findAllByStringWithPaging(OrderSearch orderSearch, int offset, int limit) {
        String jpql = "select distinct o from Order o " +
                      "join fetch o.member m " +
                      "join fetch o.delivery d " +
                      "join fetch o.orderItems oi " +
                      "join fetch oi.item i";
        
        boolean isFirstCondition = true;
        
        // 주문 상태 검색
        if (orderSearch.getOrderStatus() != null) {
            if (isFirstCondition) {
                jpql += " where";
                isFirstCondition = false;
            } else {
                jpql += " and";
            }
            jpql += " o.status = :status";
        }
        
        // 회원 이름 검색
        if (StringUtils.hasText(orderSearch.getMemberName())) {
            if (isFirstCondition) {
                jpql += " where";
                isFirstCondition = false;
            } else {
                jpql += " and";
            }
            jpql += " m.username like :name";
        }
        
        // 상품명 검색
        if (StringUtils.hasText(orderSearch.getItemName())) {
            if (isFirstCondition) {
                jpql += " where";
                isFirstCondition = false;
            } else {
                jpql += " and";
            }
            jpql += " i.name like :itemName";
        }
        
        // 카테고리명 검색
        if (StringUtils.hasText(orderSearch.getCategoryName())) {
            if (isFirstCondition) {
                jpql += " where";
                isFirstCondition = false;
            } else {
                jpql += " and";
            }
            jpql += " exists (select 1 from i.categories c where c.name = :categoryName)";
        }
        
        // 주문일시 범위 검색
        if (orderSearch.getOrderDateFrom() != null) {
            if (isFirstCondition) {
                jpql += " where";
                isFirstCondition = false;
            } else {
                jpql += " and";
            }
            jpql += " o.orderDate >= :orderDateFrom";
        }
        
        if (orderSearch.getOrderDateTo() != null) {
            if (isFirstCondition) {
                jpql += " where";
                isFirstCondition = false;
            } else {
                jpql += " and";
            }
            jpql += " o.orderDate <= :orderDateTo";
        }
        
        // 금액 범위 검색
        if (orderSearch.getMinPrice() != null) {
            if (isFirstCondition) {
                jpql += " where";
                isFirstCondition = false;
            } else {
                jpql += " and";
            }
            jpql += " (oi.orderPrice * oi.count) >= :minPrice";
        }
        
        if (orderSearch.getMaxPrice() != null) {
            if (isFirstCondition) {
                jpql += " where";
                isFirstCondition = false;
            } else {
                jpql += " and";
            }
            jpql += " (oi.orderPrice * oi.count) <= :maxPrice";
        }
        
        jpql += " order by o.orderDate desc";
        
        TypedQuery<Order> query = em.createQuery(jpql, Order.class)
            .setFirstResult(offset)
            .setMaxResults(limit);
        
        if (orderSearch.getOrderStatus() != null) {
            query.setParameter("status", orderSearch.getOrderStatus());
        }
        
        if (StringUtils.hasText(orderSearch.getMemberName())) {
            query.setParameter("name", "%" + orderSearch.getMemberName() + "%");
        }
        
        if (StringUtils.hasText(orderSearch.getItemName())) {
            query.setParameter("itemName", "%" + orderSearch.getItemName() + "%");
        }
        
        if (StringUtils.hasText(orderSearch.getCategoryName())) {
            query.setParameter("categoryName", convertCategoryToType(orderSearch.getCategoryName()));
        }
        
        if (orderSearch.getOrderDateFrom() != null) {
            query.setParameter("orderDateFrom", orderSearch.getOrderDateFrom());
        }
        
        if (orderSearch.getOrderDateTo() != null) {
            query.setParameter("orderDateTo", orderSearch.getOrderDateTo());
        }
        
        if (orderSearch.getMinPrice() != null) {
            query.setParameter("minPrice", orderSearch.getMinPrice());
        }
        
        if (orderSearch.getMaxPrice() != null) {
            query.setParameter("maxPrice", orderSearch.getMaxPrice());
        }
        
        return query.getResultList();
    }

    /**
     * 전체 주문 수 조회
     * @return 전체 주문 수
     */
    public long count() {
        return em.createQuery("select count(o) from Order o", Long.class)
            .getSingleResult();
    }

    /**
     * 검색 조건에 맞는 주문 수 조회 - 확장된 검색 조건 지원
     * @param orderSearch 검색 조건
     * @return 검색 결과 주문 수
     */
    public long countBySearch(OrderSearch orderSearch) {
        String jpql = "select count(distinct o) from Order o " +
                      "join o.member m " +
                      "join o.orderItems oi " +
                      "join oi.item i";
        
        boolean isFirstCondition = true;
        
        // 주문 상태 검색
        if (orderSearch.getOrderStatus() != null) {
            if (isFirstCondition) {
                jpql += " where";
                isFirstCondition = false;
            } else {
                jpql += " and";
            }
            jpql += " o.status = :status";
        }
        
        // 회원 이름 검색
        if (StringUtils.hasText(orderSearch.getMemberName())) {
            if (isFirstCondition) {
                jpql += " where";
                isFirstCondition = false;
            } else {
                jpql += " and";
            }
            jpql += " m.username like :name";
        }
        
        // 상품명 검색
        if (StringUtils.hasText(orderSearch.getItemName())) {
            if (isFirstCondition) {
                jpql += " where";
                isFirstCondition = false;
            } else {
                jpql += " and";
            }
            jpql += " i.name like :itemName";
        }
        
        // 카테고리명 검색
        if (StringUtils.hasText(orderSearch.getCategoryName())) {
            if (isFirstCondition) {
                jpql += " where";
                isFirstCondition = false;
            } else {
                jpql += " and";
            }
            jpql += " exists (select 1 from i.categories c where c.name = :categoryName)";
        }
        
        // 주문일시 범위 검색
        if (orderSearch.getOrderDateFrom() != null) {
            if (isFirstCondition) {
                jpql += " where";
                isFirstCondition = false;
            } else {
                jpql += " and";
            }
            jpql += " o.orderDate >= :orderDateFrom";
        }
        
        if (orderSearch.getOrderDateTo() != null) {
            if (isFirstCondition) {
                jpql += " where";
                isFirstCondition = false;
            } else {
                jpql += " and";
            }
            jpql += " o.orderDate <= :orderDateTo";
        }
        
        // 금액 범위 검색
        if (orderSearch.getMinPrice() != null) {
            if (isFirstCondition) {
                jpql += " where";
                isFirstCondition = false;
            } else {
                jpql += " and";
            }
            jpql += " (oi.orderPrice * oi.count) >= :minPrice";
        }
        
        if (orderSearch.getMaxPrice() != null) {
            if (isFirstCondition) {
                jpql += " where";
                isFirstCondition = false;
            } else {
                jpql += " and";
            }
            jpql += " (oi.orderPrice * oi.count) <= :maxPrice";
        }
        
        TypedQuery<Long> query = em.createQuery(jpql, Long.class);
        
        if (orderSearch.getOrderStatus() != null) {
            query.setParameter("status", orderSearch.getOrderStatus());
        }
        
        if (StringUtils.hasText(orderSearch.getMemberName())) {
            query.setParameter("name", "%" + orderSearch.getMemberName() + "%");
        }
        
        if (StringUtils.hasText(orderSearch.getItemName())) {
            query.setParameter("itemName", "%" + orderSearch.getItemName() + "%");
        }
        
        if (StringUtils.hasText(orderSearch.getCategoryName())) {
            query.setParameter("categoryName", convertCategoryToType(orderSearch.getCategoryName()));
        }
        
        if (orderSearch.getOrderDateFrom() != null) {
            query.setParameter("orderDateFrom", orderSearch.getOrderDateFrom());
        }
        
        if (orderSearch.getOrderDateTo() != null) {
            query.setParameter("orderDateTo", orderSearch.getOrderDateTo());
        }
        
        if (orderSearch.getMinPrice() != null) {
            query.setParameter("minPrice", orderSearch.getMinPrice());
        }
        
        if (orderSearch.getMaxPrice() != null) {
            query.setParameter("maxPrice", orderSearch.getMaxPrice());
        }
        
        return query.getSingleResult();
    }

    /**
     * JPQL로 검색 (문자열 동적 쿼리 방식) - 확장된 검색 조건 지원
     */
    public List<Order> findAllByString(OrderSearch orderSearch) {
        String jpql = "select distinct o from Order o " +
                      "join fetch o.member m " +
                      "join fetch o.delivery d " +
                      "join fetch o.orderItems oi " +
                      "join fetch oi.item i";
        
        boolean isFirstCondition = true;
        
        // 주문 상태 검색
        if (orderSearch.getOrderStatus() != null) {
            if (isFirstCondition) {
                jpql += " where";
                isFirstCondition = false;
            } else {
                jpql += " and";
            }
            jpql += " o.status = :status";
        }
        
        // 회원 이름 검색
        if (StringUtils.hasText(orderSearch.getMemberName())) {
            if (isFirstCondition) {
                jpql += " where";
                isFirstCondition = false;
            } else {
                jpql += " and";
            }
            jpql += " m.username like :name";
        }
        
        // 상품명 검색
        if (StringUtils.hasText(orderSearch.getItemName())) {
            if (isFirstCondition) {
                jpql += " where";
                isFirstCondition = false;
            } else {
                jpql += " and";
            }
            jpql += " i.name like :itemName";
        }
        
        // 카테고리명 검색
        if (StringUtils.hasText(orderSearch.getCategoryName())) {
            if (isFirstCondition) {
                jpql += " where";
                isFirstCondition = false;
            } else {
                jpql += " and";
            }
            jpql += " exists (select 1 from i.categories c where c.name = :categoryName)";
        }
        
        // 주문일시 범위 검색
        if (orderSearch.getOrderDateFrom() != null) {
            if (isFirstCondition) {
                jpql += " where";
                isFirstCondition = false;
            } else {
                jpql += " and";
            }
            jpql += " o.orderDate >= :orderDateFrom";
        }
        
        if (orderSearch.getOrderDateTo() != null) {
            if (isFirstCondition) {
                jpql += " where";
                isFirstCondition = false;
            } else {
                jpql += " and";
            }
            jpql += " o.orderDate <= :orderDateTo";
        }
        
        // 금액 범위 검색
        if (orderSearch.getMinPrice() != null) {
            if (isFirstCondition) {
                jpql += " where";
                isFirstCondition = false;
            } else {
                jpql += " and";
            }
            jpql += " (oi.orderPrice * oi.count) >= :minPrice";
        }
        
        if (orderSearch.getMaxPrice() != null) {
            if (isFirstCondition) {
                jpql += " where";
                isFirstCondition = false;
            } else {
                jpql += " and";
            }
            jpql += " (oi.orderPrice * oi.count) <= :maxPrice";
        }
        
        jpql += " order by o.orderDate desc";
        
        TypedQuery<Order> query = em.createQuery(jpql, Order.class);
        
        if (orderSearch.getOrderStatus() != null) {
            query.setParameter("status", orderSearch.getOrderStatus());
        }
        
        if (StringUtils.hasText(orderSearch.getMemberName())) {
            query.setParameter("name", "%" + orderSearch.getMemberName() + "%");
        }
        
        if (StringUtils.hasText(orderSearch.getItemName())) {
            query.setParameter("itemName", "%" + orderSearch.getItemName() + "%");
        }
        
        if (StringUtils.hasText(orderSearch.getCategoryName())) {
            query.setParameter("categoryName", convertCategoryToType(orderSearch.getCategoryName()));
        }
        
        if (orderSearch.getOrderDateFrom() != null) {
            query.setParameter("orderDateFrom", orderSearch.getOrderDateFrom());
        }
        
        if (orderSearch.getOrderDateTo() != null) {
            query.setParameter("orderDateTo", orderSearch.getOrderDateTo());
        }
        
        if (orderSearch.getMinPrice() != null) {
            query.setParameter("minPrice", orderSearch.getMinPrice());
        }
        
        if (orderSearch.getMaxPrice() != null) {
            query.setParameter("maxPrice", orderSearch.getMaxPrice());
        }
        
        return query.getResultList();
    }
    
    /**
     * JPA Criteria로 검색 (자바 코드 기반 동적 쿼리 방식)
     */
    public List<Order> findAllByCriteria(OrderSearch orderSearch) {
        // Criteria API는 fetch join을 명시적으로 지원하지 않기 때문에
        // 복잡한 경우 JPQL을 사용하는 것이 더 좋습니다.
        // 이 메서드는 보관용으로 유지하고 실제로는 findAllByString 메서드를 사용합니다.
        return findAllByString(orderSearch);
    }

    /**
     * 특정 사용자의 주문 목록 조회 (회원과 주문 상품 정보를 함께 조회)
     */
    public List<Order> findAllByMemberWithMemberAndItems(String memberEmail) {
        return em.createQuery(
                "select distinct o from Order o " +
                        "join fetch o.member m " +
                        "join fetch o.delivery d " +
                        "join fetch o.orderItems oi " +
                        "join fetch oi.item i " +
                        "where m.email = :memberEmail " +
                        "order by o.orderDate desc", Order.class)
                .setParameter("memberEmail", memberEmail)
                .getResultList();
    }

    /**
     * 특정 사용자의 주문 목록 조회 (페이지네이션)
     */
    public List<Order> findAllByMemberWithPaging(String memberEmail, int offset, int limit) {
        return em.createQuery(
                "select distinct o from Order o " +
                        "join fetch o.member m " +
                        "join fetch o.delivery d " +
                        "join fetch o.orderItems oi " +
                        "join fetch oi.item i " +
                        "where m.email = :memberEmail " +
                        "order by o.orderDate desc", Order.class)
                .setParameter("memberEmail", memberEmail)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
    }

    /**
     * 특정 사용자의 주문 개수 조회
     */
    public long countByMember(String memberEmail) {
        return em.createQuery(
                "select count(distinct o) from Order o " +
                        "join o.member m " +
                        "where m.email = :memberEmail", Long.class)
                .setParameter("memberEmail", memberEmail)
                .getSingleResult();
    }

    /**
     * 특정 사용자의 주문 검색 (검색 조건 포함) - 확장된 검색 조건 지원
     */
    public List<Order> findByMemberAndSearch(String memberEmail, OrderSearch orderSearch) {
        String jpql = "select distinct o from Order o " +
                "join fetch o.member m " +
                "join fetch o.delivery d " +
                "join fetch o.orderItems oi " +
                "join fetch oi.item i " +
                "where m.email = :memberEmail";

        // 주문 상태 조건 추가
        if (orderSearch.getOrderStatus() != null) {
            jpql += " and o.status = :status";
        }

        // 상품명 검색
        if (StringUtils.hasText(orderSearch.getItemName())) {
            jpql += " and i.name like :itemName";
        }

        // 카테고리명 검색
        if (StringUtils.hasText(orderSearch.getCategoryName())) {
            jpql += " and i.dtype = :categoryName";
        }

        // 주문일시 범위 검색
        if (orderSearch.getOrderDateFrom() != null) {
            jpql += " and o.orderDate >= :orderDateFrom";
        }

        if (orderSearch.getOrderDateTo() != null) {
            jpql += " and o.orderDate <= :orderDateTo";
        }

        // 금액 범위 검색
        if (orderSearch.getMinPrice() != null) {
            jpql += " and (oi.orderPrice * oi.count) >= :minPrice";
        }

        if (orderSearch.getMaxPrice() != null) {
            jpql += " and (oi.orderPrice * oi.count) <= :maxPrice";
        }

        jpql += " order by o.orderDate desc";

        TypedQuery<Order> query = em.createQuery(jpql, Order.class);
        query.setParameter("memberEmail", memberEmail);

        if (orderSearch.getOrderStatus() != null) {
            query.setParameter("status", orderSearch.getOrderStatus());
        }

        if (StringUtils.hasText(orderSearch.getItemName())) {
            query.setParameter("itemName", "%" + orderSearch.getItemName() + "%");
        }

        if (StringUtils.hasText(orderSearch.getCategoryName())) {
            // 카테고리는 JPQL에 직접 삽입되므로 파라미터 바인딩 불필요
        }

        if (orderSearch.getOrderDateFrom() != null) {
            query.setParameter("orderDateFrom", orderSearch.getOrderDateFrom());
        }

        if (orderSearch.getOrderDateTo() != null) {
            query.setParameter("orderDateTo", orderSearch.getOrderDateTo());
        }

        if (orderSearch.getMinPrice() != null) {
            query.setParameter("minPrice", orderSearch.getMinPrice());
        }

        if (orderSearch.getMaxPrice() != null) {
            query.setParameter("maxPrice", orderSearch.getMaxPrice());
        }

        return query.getResultList();
    }

    /**
     * 특정 사용자의 주문 검색 (페이지네이션) - 확장된 검색 조건 지원
     */
    public List<Order> findByMemberAndSearchWithPaging(String memberEmail, OrderSearch orderSearch, int offset, int limit) {
        String jpql = "select distinct o from Order o " +
                "join fetch o.member m " +
                "join fetch o.delivery d " +
                "join fetch o.orderItems oi " +
                "join fetch oi.item i " +
                "where m.email = :memberEmail";

        // 주문 상태 조건 추가
        if (orderSearch.getOrderStatus() != null) {
            jpql += " and o.status = :status";
        }

        // 상품명 검색
        if (StringUtils.hasText(orderSearch.getItemName())) {
            jpql += " and i.name like :itemName";
        }

        // 카테고리명 검색
        if (StringUtils.hasText(orderSearch.getCategoryName())) {
            jpql += " and TYPE(i) = :categoryType";
        }

        // 주문일시 범위 검색
        if (orderSearch.getOrderDateFrom() != null) {
            jpql += " and o.orderDate >= :orderDateFrom";
        }

        if (orderSearch.getOrderDateTo() != null) {
            jpql += " and o.orderDate <= :orderDateTo";
        }

        // 금액 범위 검색
        if (orderSearch.getMinPrice() != null) {
            jpql += " and (oi.orderPrice * oi.count) >= :minPrice";
        }

        if (orderSearch.getMaxPrice() != null) {
            jpql += " and (oi.orderPrice * oi.count) <= :maxPrice";
        }

        jpql += " order by o.orderDate desc";

        TypedQuery<Order> query = em.createQuery(jpql, Order.class);
        query.setParameter("memberEmail", memberEmail);

        if (orderSearch.getOrderStatus() != null) {
            query.setParameter("status", orderSearch.getOrderStatus());
        }

        if (StringUtils.hasText(orderSearch.getItemName())) {
            query.setParameter("itemName", "%" + orderSearch.getItemName() + "%");
        }

        if (StringUtils.hasText(orderSearch.getCategoryName())) {
            query.setParameter("categoryType", convertCategoryToClass(orderSearch.getCategoryName()));
        }

        if (orderSearch.getOrderDateFrom() != null) {
            query.setParameter("orderDateFrom", orderSearch.getOrderDateFrom());
        }

        if (orderSearch.getOrderDateTo() != null) {
            query.setParameter("orderDateTo", orderSearch.getOrderDateTo());
        }

        if (orderSearch.getMinPrice() != null) {
            query.setParameter("minPrice", orderSearch.getMinPrice());
        }

        if (orderSearch.getMaxPrice() != null) {
            query.setParameter("maxPrice", orderSearch.getMaxPrice());
        }

        return query.setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
    }

    /**
     * 특정 사용자의 주문 검색 개수 - 확장된 검색 조건 지원
     */
    public long countByMemberAndSearch(String memberEmail, OrderSearch orderSearch) {
        String jpql = "select count(distinct o) from Order o " +
                "join o.member m " +
                "join o.orderItems oi " +
                "join oi.item i " +
                "where m.email = :memberEmail";

        // 주문 상태 조건 추가
        if (orderSearch.getOrderStatus() != null) {
            jpql += " and o.status = :status";
        }

        // 상품명 검색
        if (StringUtils.hasText(orderSearch.getItemName())) {
            jpql += " and i.name like :itemName";
        }

        // 카테고리명 검색
        if (StringUtils.hasText(orderSearch.getCategoryName())) {
            jpql += " and TYPE(i) = :categoryType";
        }

        // 주문일시 범위 검색
        if (orderSearch.getOrderDateFrom() != null) {
            jpql += " and o.orderDate >= :orderDateFrom";
        }

        if (orderSearch.getOrderDateTo() != null) {
            jpql += " and o.orderDate <= :orderDateTo";
        }

        // 금액 범위 검색
        if (orderSearch.getMinPrice() != null) {
            jpql += " and (oi.orderPrice * oi.count) >= :minPrice";
        }

        if (orderSearch.getMaxPrice() != null) {
            jpql += " and (oi.orderPrice * oi.count) <= :maxPrice";
        }

        TypedQuery<Long> query = em.createQuery(jpql, Long.class);
        query.setParameter("memberEmail", memberEmail);

        if (orderSearch.getOrderStatus() != null) {
            query.setParameter("status", orderSearch.getOrderStatus());
        }

        if (StringUtils.hasText(orderSearch.getItemName())) {
            query.setParameter("itemName", "%" + orderSearch.getItemName() + "%");
        }

        if (StringUtils.hasText(orderSearch.getCategoryName())) {
            query.setParameter("categoryType", convertCategoryToClass(orderSearch.getCategoryName()));
        }

        if (orderSearch.getOrderDateFrom() != null) {
            query.setParameter("orderDateFrom", orderSearch.getOrderDateFrom());
        }

        if (orderSearch.getOrderDateTo() != null) {
            query.setParameter("orderDateTo", orderSearch.getOrderDateTo());
        }

        if (orderSearch.getMinPrice() != null) {
            query.setParameter("minPrice", orderSearch.getMinPrice());
        }

        if (orderSearch.getMaxPrice() != null) {
            query.setParameter("maxPrice", orderSearch.getMaxPrice());
        }

        return query.getSingleResult();
    }
    
    /**
     * 디버깅용: 모든 카테고리 조회
     */
    public List<String> findAllCategories() {
        return em.createQuery(
                "select distinct i.dtype from Item i where i.dtype is not null order by i.dtype", String.class)
                .getResultList();
    }
    
    /**
     * 한글 카테고리명을 dtype 코드로 변환
     */
    private String convertCategoryToType(String categoryName) {
        String result;
        switch (categoryName) {
            case "음반":
                result = "A";
                break;
            case "도서":
                result = "B";
                break;
            case "영화":
                result = "M";
                break;
            default:
                result = categoryName; // 원본 그대로 반환
        }
        System.out.println("🔍 카테고리 변환: '" + categoryName + "' → '" + result + "'");
        return result;
    }

    /**
     * 한글 카테고리명을 클래스 타입으로 변환
     */
    private Class<?> convertCategoryToClass(String categoryName) {
        Class<?> result;
        switch (categoryName) {
            case "음반": result = com.example.backend.item.domain.Album.class; break;
            case "도서": result = com.example.backend.item.domain.Book.class; break;
            case "영화": result = com.example.backend.item.domain.Movie.class; break;
            default: result = com.example.backend.item.domain.Item.class;
        }
        System.out.println("🔍 카테고리 클래스 변환: '" + categoryName + "' → '" + result.getSimpleName() + "'");
        return result;
    }

}

