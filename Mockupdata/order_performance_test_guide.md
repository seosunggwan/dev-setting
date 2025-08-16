# 🛒 Order 모듈 성능 테스트 완벽 가이드

## 🎯 목적
- 전자상거래 주문 시스템의 대용량 처리 성능 측정
- 복잡한 JOIN 쿼리 및 페이지네이션 최적화
- 주문/결제/배송 프로세스 통합 성능 검증

## 📊 생성되는 테스트 데이터 현황

| 테이블 | 데이터 수 | 설명 |
|--------|-----------|------|
| `item` | +1,000개 | 주문할 상품 (책/앨범/영화) |
| `delivery` | 50,000개 | 배송 정보 및 주소 |
| `orders` | 30,000개 | 주문 정보 (ORDER/CANCEL) |
| `order_item` | 80,000개 | 주문 상품 상세 (평균 2.67개/주문) |

## 🚀 실행 방법

### 1. 사전 준비
```bash
# 백엔드 서버 중지
docker-compose stop app

# 데이터베이스 백업
mysqldump -u username -p database_name > backup_order_test.sql
```

### 2. 의존성 데이터 확인
```bash
# User 및 Item 기본 데이터가 있어야 함
# Board와 Chat 테스트 데이터가 먼저 실행되었다면 OK
# 아니면 기본 사용자 100명과 상품 100개 정도는 있어야 함
```

### 3. 테스트 데이터 생성
```bash
# Order 테스트 데이터 실행 (약 15-20분 소요)
mysql -u username -p database_name < order_test_data.sql
```

### 4. 서버 재시작
```bash
docker-compose start app
```

## 🔥 핵심 성능 테스트 시나리오

### 1. 🛍️ 주문 목록 조회 성능 테스트

#### **전체 주문 목록 (페이지네이션)**
```sql
-- 기본 주문 목록 조회 (가장 많이 사용되는 쿼리)
EXPLAIN ANALYZE 
SELECT DISTINCT o.order_id, o.order_date, o.status, 
       m.username, d.city, d.status as delivery_status
FROM orders o 
JOIN user_entity m ON o.member_id = m.id 
JOIN delivery d ON o.delivery_id = d.delivery_id 
ORDER BY o.order_date DESC 
LIMIT 20 OFFSET 100;
```

#### **복잡한 JOIN 쿼리 (fetch join 시뮬레이션)**
```sql
-- OrderRepository.findAllWithMemberAndItems() 쿼리 테스트
EXPLAIN ANALYZE 
SELECT DISTINCT o.order_id, o.order_date, o.status,
       m.username, m.email,
       d.status as delivery_status, d.city, d.street,
       oi.order_price, oi.count,
       i.name as item_name, i.price as item_price
FROM orders o 
JOIN user_entity m ON o.member_id = m.id 
JOIN delivery d ON o.delivery_id = d.delivery_id 
JOIN order_item oi ON o.order_id = oi.order_id 
JOIN item i ON oi.item_id = i.item_id 
ORDER BY o.order_date DESC 
LIMIT 50;
```

### 2. 🔍 주문 검색 성능 테스트

#### **사용자별 주문 조회**
```sql
-- OrderRepository.findAllByMemberWithMemberAndItems() 쿼리
EXPLAIN ANALYZE 
SELECT DISTINCT o.order_id, o.order_date, o.status,
       m.username, d.city, oi.order_price, i.name
FROM orders o 
JOIN user_entity m ON o.member_id = m.id 
JOIN delivery d ON o.delivery_id = d.delivery_id 
JOIN order_item oi ON o.order_id = oi.order_id 
JOIN item i ON oi.item_id = i.item_id 
WHERE m.email = 'test@example.com' 
ORDER BY o.order_date DESC;
```

#### **동적 쿼리 검색 (상태 + 사용자명)**
```sql
-- OrderRepository.findAllByStringWithPaging() 쿼리
EXPLAIN ANALYZE 
SELECT DISTINCT o.order_id, o.order_date, o.status, m.username
FROM orders o 
JOIN user_entity m ON o.member_id = m.id 
JOIN delivery d ON o.delivery_id = d.delivery_id 
JOIN order_item oi ON o.order_id = oi.order_id 
JOIN item i ON oi.item_id = i.item_id 
WHERE o.status = 'ORDER' 
  AND m.username LIKE '%test%' 
ORDER BY o.order_date DESC 
LIMIT 20 OFFSET 0;
```

#### **날짜 범위 검색**
```sql
-- 특정 기간 주문 조회
EXPLAIN ANALYZE 
SELECT COUNT(*) as order_count,
       SUM(oi.order_price * oi.count) as total_revenue
FROM orders o 
JOIN order_item oi ON o.order_id = oi.order_id 
WHERE o.status = 'ORDER' 
  AND o.order_date BETWEEN '2024-01-01' AND '2024-12-31';
```

### 3. 📈 주문 통계 성능 테스트

#### **일별 주문 통계**
```sql
-- 월별/일별 주문 집계 쿼리
EXPLAIN ANALYZE 
SELECT 
    DATE(o.order_date) as order_date,
    COUNT(DISTINCT o.order_id) as daily_orders,
    COUNT(DISTINCT o.member_id) as unique_customers,
    SUM(oi.order_price * oi.count) as daily_revenue,
    AVG(oi.order_price * oi.count) as avg_order_value
FROM orders o 
JOIN order_item oi ON o.order_id = oi.order_id 
WHERE o.status = 'ORDER'
  AND o.order_date >= DATE_SUB(NOW(), INTERVAL 30 DAY)
GROUP BY DATE(o.order_date)
ORDER BY order_date DESC;
```

#### **인기 상품 랭킹**
```sql
-- 상품별 주문 통계 (가장 복잡한 집계 쿼리)
EXPLAIN ANALYZE 
SELECT 
    i.name,
    i.price,
    COUNT(DISTINCT o.order_id) as order_count,
    SUM(oi.count) as total_quantity_sold,
    SUM(oi.order_price * oi.count) as total_revenue,
    AVG(oi.order_price) as avg_selling_price,
    COUNT(DISTINCT o.member_id) as unique_buyers
FROM item i 
JOIN order_item oi ON i.item_id = oi.item_id 
JOIN orders o ON oi.order_id = o.order_id 
WHERE o.status = 'ORDER'
  AND o.order_date >= DATE_SUB(NOW(), INTERVAL 90 DAY)
GROUP BY i.item_id, i.name, i.price
HAVING order_count >= 5  -- 최소 5번 이상 주문된 상품
ORDER BY total_revenue DESC 
LIMIT 50;
```

#### **고객별 주문 패턴 분석**
```sql
-- 고객 세그멘테이션 쿼리
EXPLAIN ANALYZE 
SELECT 
    m.username,
    m.email,
    COUNT(DISTINCT o.order_id) as total_orders,
    SUM(oi.order_price * oi.count) as total_spent,
    AVG(oi.order_price * oi.count) as avg_order_value,
    MIN(o.order_date) as first_order_date,
    MAX(o.order_date) as last_order_date,
    COUNT(DISTINCT i.item_id) as unique_items_bought
FROM user_entity m 
JOIN orders o ON m.id = o.member_id 
JOIN order_item oi ON o.order_id = oi.order_id 
JOIN item i ON oi.item_id = i.item_id 
WHERE o.status = 'ORDER'
GROUP BY m.id, m.username, m.email
HAVING total_orders >= 3  -- 3번 이상 주문한 고객
ORDER BY total_spent DESC 
LIMIT 100;
```

### 4. 🚚 배송 관련 성능 테스트

#### **배송 상태별 통계**
```sql
-- 배송 현황 대시보드 쿼리
EXPLAIN ANALYZE 
SELECT 
    d.status as delivery_status,
    d.city,
    COUNT(*) as delivery_count,
    COUNT(CASE WHEN o.status = 'ORDER' THEN 1 END) as active_orders,
    COUNT(CASE WHEN o.status = 'CANCEL' THEN 1 END) as cancelled_orders
FROM delivery d 
JOIN orders o ON d.delivery_id = o.delivery_id 
GROUP BY d.status, d.city
ORDER BY delivery_count DESC;
```

#### **지역별 배송 분석**
```sql
-- 지역별 주문 패턴
EXPLAIN ANALYZE 
SELECT 
    d.city,
    COUNT(DISTINCT o.order_id) as total_orders,
    SUM(oi.order_price * oi.count) as total_revenue,
    AVG(oi.order_price * oi.count) as avg_order_value,
    COUNT(CASE WHEN d.status = 'COMP' THEN 1 END) as completed_deliveries
FROM delivery d 
JOIN orders o ON d.delivery_id = o.delivery_id 
JOIN order_item oi ON o.order_id = oi.order_id 
WHERE o.status = 'ORDER'
GROUP BY d.city
ORDER BY total_revenue DESC 
LIMIT 20;
```

### 5. 🔄 실시간 주문 처리 테스트

#### **단일 주문 상세 조회**
```sql
-- OrderRepository.findOrderWithMemberAndItems() 쿼리
EXPLAIN ANALYZE 
SELECT o.order_id, o.order_date, o.status,
       m.username, m.email,
       d.status as delivery_status, d.city, d.street, d.zipcode,
       oi.order_price, oi.count,
       i.name as item_name, i.price as item_price
FROM orders o 
JOIN user_entity m ON o.member_id = m.id 
JOIN delivery d ON o.delivery_id = d.delivery_id 
JOIN order_item oi ON o.order_id = oi.order_id 
JOIN item i ON oi.item_id = i.item_id 
WHERE o.order_id = 15000;
```

#### **재고 확인 쿼리**
```sql
-- 주문 가능 상품 확인
EXPLAIN ANALYZE 
SELECT i.item_id, i.name, i.price, i.stock_quantity,
       COALESCE(SUM(oi.count), 0) as total_ordered,
       (i.stock_quantity - COALESCE(SUM(oi.count), 0)) as available_stock
FROM item i 
LEFT JOIN order_item oi ON i.item_id = oi.item_id 
LEFT JOIN orders o ON oi.order_id = o.order_id AND o.status = 'ORDER'
WHERE i.stock_quantity > 0
GROUP BY i.item_id, i.name, i.price, i.stock_quantity
HAVING available_stock > 0
ORDER BY available_stock ASC 
LIMIT 100;
```

## ⚡ 실시간 성능 최적화 포인트

### 1. **쿼리 최적화**
```sql
-- 인덱스 사용률 확인
SHOW INDEX FROM orders;
SHOW INDEX FROM order_item;
SHOW INDEX FROM delivery;

-- 실행 계획 분석
EXPLAIN FORMAT=JSON 
SELECT DISTINCT o.order_id, o.order_date, m.username 
FROM orders o 
JOIN user_entity m ON o.member_id = m.id 
ORDER BY o.order_date DESC 
LIMIT 20;
```

### 2. **배치 처리 시뮬레이션**
```sql
-- 대량 주문 상태 업데이트 (배치 작업)
UPDATE orders 
SET status = 'CANCEL' 
WHERE order_id IN (
    SELECT order_id FROM (
        SELECT o.order_id 
        FROM orders o 
        WHERE o.order_date < DATE_SUB(NOW(), INTERVAL 30 DAY)
          AND o.status = 'ORDER'
        LIMIT 1000
    ) old_orders
);
```

### 3. **캐싱 전략**
```redis
# 인기 상품 랭킹 캐싱
SET popular:products:30days "[{상품 JSON 배열}]" EX 3600

# 사용자별 주문 통계 캐싱
SET user:123:order:stats "{total_orders: 10, total_spent: 500000}" EX 300

# 일일 주문 통계 캐싱
SET daily:stats:2024-01-15 "{orders: 250, revenue: 15000000}" EX 86400
```

## 🎯 성능 지표 및 임계값

### **응답 시간 목표**
- **주문 목록 조회**: < 200ms
- **주문 상세 조회**: < 100ms
- **주문 검색**: < 300ms
- **통계 쿼리**: < 500ms

### **처리량 목표**
- **동시 주문 처리**: 100건/초
- **주문 목록 조회**: 500건/초
- **일일 주문량**: 50,000건
- **최대 주문 항목**: 10개/주문

## 🔧 최적화 전략

### 1. **인덱스 최적화**
```sql
-- 복합 인덱스 효과 확인
ANALYZE TABLE orders;
ANALYZE TABLE order_item;
ANALYZE TABLE delivery;

-- 인덱스 힌트 사용 테스트
SELECT /*+ USE_INDEX(orders, idx_orders_member_date) */ 
       o.order_id, o.order_date 
FROM orders o 
WHERE o.member_id = 1 
ORDER BY o.order_date DESC;
```

### 2. **파티셔닝 전략**
```sql
-- 주문 테이블 월별 파티셔닝 (대용량 처리시)
ALTER TABLE orders 
PARTITION BY RANGE (YEAR(order_date) * 100 + MONTH(order_date)) (
  PARTITION p202401 VALUES LESS THAN (202402),
  PARTITION p202402 VALUES LESS THAN (202403),
  -- ... 계속
  PARTITION p_future VALUES LESS THAN MAXVALUE
);
```

### 3. **읽기 전용 복제본 활용**
```sql
-- 통계 쿼리는 읽기 전용 DB에서 실행
-- 실시간 주문 처리는 마스터 DB에서 실행
```

## 📊 모니터링 쿼리

### **실시간 성능 모니터링**
```sql
-- 느린 쿼리 확인
SELECT digest_text, avg_timer_wait/1000000000 as avg_time_sec, count_star 
FROM performance_schema.events_statements_summary_by_digest 
WHERE schema_name = 'portfolio' 
  AND avg_timer_wait > 1000000000  -- 1초 이상
ORDER BY avg_timer_wait DESC 
LIMIT 10;

-- 테이블 크기 모니터링
SELECT table_name, 
       ROUND(((data_length + index_length) / 1024 / 1024), 2) AS 'Size in MB',
       table_rows
FROM information_schema.tables 
WHERE table_schema = 'portfolio' 
  AND table_name IN ('orders', 'order_item', 'delivery')
ORDER BY (data_length + index_length) DESC;

-- 락 대기 상황 확인
SELECT r.trx_id waiting_trx_id,
       r.trx_mysql_thread_id waiting_thread,
       r.trx_query waiting_query,
       b.trx_id blocking_trx_id,
       b.trx_mysql_thread_id blocking_thread,
       b.trx_query blocking_query
FROM information_schema.innodb_lock_waits w
JOIN information_schema.innodb_trx b ON b.trx_id = w.blocking_trx_id
JOIN information_schema.innodb_trx r ON r.trx_id = w.requesting_trx_id;
```

## 🧪 스트레스 테스트 시나리오

### 1. **동시 주문 생성 테스트**
```bash
# 100명이 동시에 주문 생성
for i in {1..100}; do
  curl -X POST http://localhost:8080/api/orders \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer ${token}" \
    -d '{
      "memberId": '$((i % 50 + 1))',
      "orderItems": [
        {"itemId": '$((i % 100 + 1))', "count": '$((i % 3 + 1))'}
      ]
    }' &
done
```

### 2. **주문 목록 조회 폭주**
```bash
# 여러 사용자가 동시에 주문 목록 조회
for page in {0..50}; do
  curl "http://localhost:8080/api/orders?page=${page}&size=20&status=ORDER" \
    -H "Authorization: Bearer ${token}" &
done
```

### 3. **통계 쿼리 스트레스 테스트**
```bash
# 관리자 대시보드 동시 접속
for i in {1..20}; do
  curl "http://localhost:8080/api/admin/orders/statistics?days=30" \
    -H "Authorization: Bearer ${admin_token}" &
done
```

## 🎉 결과 분석

### **성공 기준**
- ✅ 모든 쿼리 응답시간 < 1초
- ✅ 동시 주문 100건 처리 가능
- ✅ 페이지네이션 성능 일정
- ✅ 통계 쿼리 메모리 사용량 안정

### **최적화 우선순위**
1. **주문 목록 조회** - 가장 빈번한 작업
2. **복잡한 JOIN 쿼리** - 가장 무거운 작업
3. **통계 집계** - 리소스 집약적
4. **검색 기능** - 사용자 경험 중요

### **예상 병목점**
- `orders` + `order_item` + `item` JOIN 쿼리
- `DISTINCT` 사용으로 인한 임시 테이블 생성
- 복잡한 GROUP BY 집계 연산
- 페이지네이션 OFFSET 성능 저하

이 가이드로 주문 시스템의 모든 성능 이슈를 체계적으로 분석하고 최적화할 수 있습니다! 🚀
