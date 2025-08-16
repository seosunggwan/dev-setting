# 📊 게시판 모듈 인덱스 최적화를 위한 테스트 데이터 가이드

## 🎯 목적
- 게시판 모듈의 다양한 쿼리 성능 테스트
- 인덱스 최적화 효과 측정
- 대용량 데이터 환경에서의 성능 분석

## 📈 생성되는 데이터 현황

| 테이블 | 데이터 수 | 설명 |
|--------|-----------|------|
| `user_entity` | 100개 | 테스트용 사용자 |
| `board` | 10,000개 | 다양한 패턴의 게시글 |
| `comment` | 50,000개 | 계층형 댓글 (90% 원댓글, 10% 대댓글) |
| `board_like` | ~100,000개 | 좋아요 데이터 (중복 제거) |
| `popular_boards` | ~300개 | 최근 30일간 인기글 (일당 10개) |

## 🚀 실행 방법

### 1. 데이터베이스 백업 (권장)
```bash
mysqldump -u [username] -p [database_name] > backup_before_test.sql
```

### 2. 테스트 데이터 실행
```bash
mysql -u [username] -p [database_name] < board_test_data.sql
```

### 3. 실행 시간 예상
- **소규모 서버**: 5-10분
- **중간 서버**: 2-5분  
- **고성능 서버**: 1-2분

## ⚠️ 주의사항

### 실행 전 확인사항
1. **기존 데이터 백업** 필수
2. **충분한 디스크 공간** 확인 (약 1-2GB 필요)
3. **MySQL 설정 확인**:
   ```sql
   SET SESSION max_execution_time = 0;
   SET SESSION wait_timeout = 28800;
   ```

### 데이터 특성
- **현실적인 분포**: 인기글에 조회수/좋아요가 집중
- **시간대별 분포**: 최근 게시글에 활동이 더 많음
- **사용자 분포**: 일부 활성 사용자가 더 많은 활동

## 📊 성능 테스트 쿼리

### 1. 제목 검색 성능
```sql
EXPLAIN ANALYZE 
SELECT * FROM board 
WHERE title LIKE '%인기글%' 
ORDER BY created_time DESC 
LIMIT 20;
```

### 2. 내용 검색 성능
```sql
EXPLAIN ANALYZE 
SELECT * FROM board 
WHERE content LIKE '%공지사항%' 
ORDER BY view_count DESC 
LIMIT 20;
```

### 3. 복합 검색 성능 (실제 BoardRepository 쿼리)
```sql
EXPLAIN ANALYZE 
SELECT b FROM board b 
WHERE b.title LIKE '%팁%' OR b.content LIKE '%팁%' 
ORDER BY b.created_time DESC 
LIMIT 20;
```

### 4. 인기글 선정 쿼리 (PopularBoardService)
```sql
EXPLAIN ANALYZE 
SELECT * FROM board 
WHERE created_time >= DATE_SUB(NOW(), INTERVAL 30 DAY) 
ORDER BY (view_count * 0.3 + like_count * 0.7) DESC 
LIMIT 10;
```

### 5. 댓글 조회 성능 (CommentRepository)
```sql
EXPLAIN ANALYZE 
SELECT * FROM comment 
WHERE board_id = 5000 AND parent_id IS NULL 
ORDER BY created_time ASC;
```

### 6. 좋아요 개수 조회
```sql
EXPLAIN ANALYZE 
SELECT COUNT(*) FROM board_like 
WHERE board_id = 5000;
```

## 🔍 인덱스 효과 측정

### Before/After 비교 방법

1. **인덱스 제거 후 성능 측정**:
```sql
DROP INDEX idx_board_title ON board;
DROP INDEX idx_board_content ON board;
-- 성능 테스트 실행
```

2. **인덱스 재생성 후 성능 측정**:
```sql
CREATE INDEX idx_board_title ON board(title);
CREATE INDEX idx_board_content ON board(content(100));
-- 성능 테스트 실행
```

3. **실행 계획 분석**:
```sql
EXPLAIN FORMAT=JSON SELECT ...;
```

## 📈 성능 지표 확인

### 중요 지표
- **실행 시간** (execution time)
- **검사된 행 수** (rows examined)
- **인덱스 사용 여부** (key used)
- **조인 타입** (join type)

### 모니터링 쿼리
```sql
-- 느린 쿼리 확인
SHOW GLOBAL STATUS LIKE 'Slow_queries';

-- 인덱스 사용률 확인
SHOW GLOBAL STATUS LIKE 'Handler_read%';

-- 테이블 상태 확인
SHOW TABLE STATUS LIKE 'board';
```

## 🧹 데이터 정리

### 테스트 완료 후 데이터 삭제
```sql
-- 테스트 데이터만 삭제 (주의: 기존 데이터도 함께 삭제됨)
DELETE FROM popular_boards;
DELETE FROM board_like;
DELETE FROM comment;
DELETE FROM board WHERE title LIKE '%테스트%' OR title LIKE '%인기글%';
DELETE FROM user_entity WHERE email LIKE '%testuser%';

-- 또는 백업에서 복원
-- mysql -u [username] -p [database_name] < backup_before_test.sql
```

## 💡 최적화 팁

### 1. 인덱스 튜닝
- **단일 컬럼 인덱스**: 자주 검색되는 컬럼
- **복합 인덱스**: WHERE + ORDER BY 조건
- **부분 인덱스**: TEXT 컬럼의 앞부분만

### 2. 쿼리 최적화
- **LIKE 검색**: Full-Text Search 고려
- **정렬 조건**: 인덱스와 일치하는 순서
- **JOIN 최적화**: 적절한 인덱스 설정

### 3. 애플리케이션 레벨 최적화
- **페이지네이션**: OFFSET 대신 커서 기반
- **캐싱**: Redis를 활용한 결과 캐싱
- **배치 처리**: 대량 작업은 배치로 처리

## 🔧 추가 테스트 시나리오

### 1. 동시성 테스트
```bash
# 여러 세션에서 동시 검색
for i in {1..10}; do
  mysql -u user -p db -e "SELECT * FROM board WHERE title LIKE '%인기글%' LIMIT 100;" &
done
```

### 2. 메모리 사용량 테스트
```sql
-- 메모리 사용량 확인
SHOW GLOBAL STATUS LIKE 'Innodb_buffer_pool%';
```

### 3. 락 경합 테스트
```sql
-- 락 상태 확인
SHOW ENGINE INNODB STATUS;
```

이 가이드를 따라 체계적으로 성능 테스트를 진행하시면 최적의 인덱스 전략을 수립할 수 있습니다! 🚀
