# 💬 Chat 모듈 성능 테스트 완벽 가이드

## 🎯 목적
- 실시간 채팅 시스템의 대용량 트래픽 성능 측정
- 웹소켓 연결 및 메시지 처리 최적화
- 읽음 상태 관리 및 알림 시스템 성능 검증

## 📊 생성되는 테스트 데이터 현황

| 테이블 | 데이터 수 | 설명 |
|--------|-----------|------|
| `user_entity` | +500개 | 채팅 전용 사용자 (기존 + 추가) |
| `chat_room` | 2,000개 | 1:1 채팅(1,500) + 그룹채팅(500) |
| `chat_participant` | 10,000개 | 채팅방 참여자 관계 |
| `chat_message` | 100,000개 | 다양한 패턴의 메시지 |
| `read_status` | 500,000개 | 메시지별 읽음 상태 |

## 🚀 실행 방법

### 1. 사전 준비
```bash
# 백엔드 서버 중지 (안전한 데이터 작업을 위해)
docker-compose stop app

# 데이터베이스 백업
mysqldump -u username -p database_name > backup_chat_test.sql
```

### 2. 테스트 데이터 생성
```bash
# Board 테스트 데이터가 있어야 함 (사용자 데이터 의존성)
mysql -u username -p database_name < board_test_data.sql

# Chat 테스트 데이터 생성 (약 10-15분 소요)
mysql -u username -p database_name < chat_test_data.sql
```

### 3. 서버 재시작
```bash
docker-compose start app
```

## 🔥 핵심 성능 테스트 시나리오

### 1. 💬 메시지 처리 성능 테스트

#### **대량 메시지 조회**
```sql
-- 채팅방의 최근 메시지 100개 조회 (페이지네이션)
EXPLAIN ANALYZE 
SELECT cm.*, u.username 
FROM chat_message cm 
JOIN user_entity u ON cm.member_id = u.id 
WHERE cm.chat_room_id = 1500 
ORDER BY cm.created_time DESC 
LIMIT 100;
```

#### **실시간 메시지 스트림**
```sql
-- 특정 시간 이후의 새 메시지 조회
EXPLAIN ANALYZE 
SELECT * FROM chat_message 
WHERE chat_room_id = 1500 
  AND created_time > '2024-01-01 10:00:00' 
ORDER BY created_time ASC;
```

#### **메시지 검색 성능**
```sql
-- 채팅방 내 메시지 내용 검색
EXPLAIN ANALYZE 
SELECT * FROM chat_message 
WHERE chat_room_id = 1500 
  AND content LIKE '%안녕%' 
ORDER BY created_time DESC 
LIMIT 50;
```

### 2. 👥 채팅방 관리 성능 테스트

#### **사용자별 채팅방 목록**
```sql
-- 특정 사용자가 참여한 모든 채팅방
EXPLAIN ANALYZE 
SELECT cr.*, 
       (SELECT COUNT(*) FROM chat_message cm WHERE cm.chat_room_id = cr.id) as message_count,
       (SELECT COUNT(*) FROM read_status rs WHERE rs.chat_room_id = cr.id AND rs.member_id = 1 AND rs.is_read = FALSE) as unread_count
FROM chat_room cr 
JOIN chat_participant cp ON cr.id = cp.chat_room_id 
WHERE cp.member_id = 1 
ORDER BY cr.modified_time DESC;
```

#### **그룹 채팅방 검색**
```sql
-- 그룹 채팅방 이름으로 검색 (페이지네이션)
EXPLAIN ANALYZE 
SELECT * FROM chat_room 
WHERE is_group_chat = 'Y' 
  AND name LIKE '%업무%' 
ORDER BY created_time DESC 
LIMIT 20 OFFSET 40;
```

#### **1:1 채팅방 찾기 (중복 방지)**
```sql
-- 두 사용자 간의 기존 1:1 채팅방 찾기
EXPLAIN ANALYZE 
SELECT cp1.chat_room_id 
FROM chat_participant cp1 
JOIN chat_participant cp2 ON cp1.chat_room_id = cp2.chat_room_id 
JOIN chat_room cr ON cp1.chat_room_id = cr.id 
WHERE cp1.member_id = 1 
  AND cp2.member_id = 2 
  AND cr.is_group_chat = 'N';
```

### 3. 📬 읽음 상태 관리 성능 테스트

#### **안읽은 메시지 통계**
```sql
-- 사용자별 전체 안읽은 메시지 개수
EXPLAIN ANALYZE 
SELECT COUNT(*) as total_unread 
FROM read_status 
WHERE member_id = 1 
  AND is_read = FALSE;
```

#### **채팅방별 안읽은 메시지**
```sql
-- 채팅방별 안읽은 메시지 개수 (대시보드용)
EXPLAIN ANALYZE 
SELECT cr.name, COUNT(*) as unread_count 
FROM read_status rs 
JOIN chat_room cr ON rs.chat_room_id = cr.id 
WHERE rs.member_id = 1 
  AND rs.is_read = FALSE 
GROUP BY rs.chat_room_id 
ORDER BY unread_count DESC;
```

#### **읽음 상태 일괄 업데이트**
```sql
-- 채팅방 입장시 모든 메시지 읽음 처리
EXPLAIN ANALYZE 
UPDATE read_status 
SET is_read = TRUE, modified_time = NOW() 
WHERE chat_room_id = 1500 
  AND member_id = 1 
  AND is_read = FALSE;
```

### 4. 📈 채팅 활동 통계 성능 테스트

#### **활발한 채팅방 순위**
```sql
-- 최근 7일간 메시지가 많은 채팅방
EXPLAIN ANALYZE 
SELECT cr.name, cr.is_group_chat, COUNT(cm.id) as recent_messages 
FROM chat_room cr 
LEFT JOIN chat_message cm ON cr.id = cm.chat_room_id 
WHERE cm.created_time >= DATE_SUB(NOW(), INTERVAL 7 DAY) 
GROUP BY cr.id 
ORDER BY recent_messages DESC 
LIMIT 20;
```

#### **사용자 활동 통계**
```sql
-- 사용자별 채팅 활동 통계
EXPLAIN ANALYZE 
SELECT u.username,
       COUNT(DISTINCT cp.chat_room_id) as joined_rooms,
       COUNT(DISTINCT cm.id) as sent_messages,
       COUNT(DISTINCT DATE(cm.created_time)) as active_days
FROM user_entity u 
LEFT JOIN chat_participant cp ON u.id = cp.member_id 
LEFT JOIN chat_message cm ON u.id = cm.member_id 
WHERE cm.created_time >= DATE_SUB(NOW(), INTERVAL 30 DAY) 
GROUP BY u.id 
ORDER BY sent_messages DESC 
LIMIT 50;
```

#### **시간대별 채팅 패턴**
```sql
-- 시간대별 메시지 빈도 분석
EXPLAIN ANALYZE 
SELECT HOUR(created_time) as hour, COUNT(*) as message_count 
FROM chat_message 
WHERE created_time >= DATE_SUB(NOW(), INTERVAL 30 DAY) 
GROUP BY HOUR(created_time) 
ORDER BY hour;
```

## ⚡ 실시간 성능 최적화 포인트

### 1. **웹소켓 연결 관리**
```javascript
// 프론트엔드 테스트: 동시 연결 시뮬레이션
for (let i = 0; i < 100; i++) {
  const socket = new WebSocket('ws://localhost:8080/api/ws');
  socket.onmessage = (event) => {
    console.log(`User ${i} received:`, event.data);
  };
}
```

### 2. **메시지 캐싱 전략**
```sql
-- Redis 캐싱용 쿼리 (최근 메시지 50개)
SELECT cm.*, u.username 
FROM chat_message cm 
JOIN user_entity u ON cm.member_id = u.id 
WHERE cm.chat_room_id = ? 
ORDER BY cm.created_time DESC 
LIMIT 50;
```

### 3. **읽음 상태 배치 처리**
```sql
-- 대량 읽음 상태 업데이트 (배치 처리)
UPDATE read_status rs 
JOIN chat_message cm ON rs.chat_message_id = cm.id 
SET rs.is_read = TRUE, rs.modified_time = NOW() 
WHERE rs.chat_room_id = ? 
  AND rs.member_id = ? 
  AND cm.created_time <= ? 
  AND rs.is_read = FALSE;
```

## 🎯 성능 지표 및 임계값

### **응답 시간 목표**
- **메시지 조회**: < 100ms
- **메시지 전송**: < 50ms  
- **읽음 상태 업데이트**: < 200ms
- **채팅방 목록**: < 150ms

### **동시 처리 목표**
- **동시 접속자**: 1,000명
- **초당 메시지**: 500건
- **채팅방 수**: 10,000개
- **일일 메시지**: 1,000,000건

## 🔧 최적화 전략

### 1. **인덱스 최적화**
```sql
-- 가장 중요한 인덱스들
SHOW INDEX FROM chat_message WHERE Key_name LIKE 'idx_%';
SHOW INDEX FROM read_status WHERE Key_name LIKE 'idx_%';
SHOW INDEX FROM chat_participant WHERE Key_name LIKE 'idx_%';
```

### 2. **파티셔닝 전략**
```sql
-- 메시지 테이블 월별 파티셔닝 (대용량 처리시)
ALTER TABLE chat_message 
PARTITION BY RANGE (YEAR(created_time) * 100 + MONTH(created_time)) (
  PARTITION p202401 VALUES LESS THAN (202402),
  PARTITION p202402 VALUES LESS THAN (202403),
  -- ... 계속
  PARTITION p_future VALUES LESS THAN MAXVALUE
);
```

### 3. **Redis 캐싱**
```redis
# 활성 채팅방 캐싱
SET chat:room:1500:messages "[{메시지 JSON 배열}]" EX 300

# 안읽은 메시지 개수 캐싱  
SET user:1:unread:count "25" EX 60

# 온라인 사용자 관리
SADD online:users "user:1" "user:2" "user:3"
```

## 📊 모니터링 쿼리

### **실시간 성능 모니터링**
```sql
-- 느린 쿼리 확인
SELECT * FROM performance_schema.events_statements_summary_by_digest 
WHERE SCHEMA_NAME = 'portfolio' 
ORDER BY AVG_TIMER_WAIT DESC 
LIMIT 10;

-- 테이블 크기 확인
SELECT table_name, 
       ROUND(((data_length + index_length) / 1024 / 1024), 2) AS 'DB Size in MB' 
FROM information_schema.tables 
WHERE table_schema = 'portfolio' 
  AND table_name LIKE 'chat_%' 
ORDER BY (data_length + index_length) DESC;

-- 인덱스 사용률 확인
SELECT object_schema, object_name, index_name, count_read, count_write 
FROM performance_schema.table_io_waits_summary_by_index_usage 
WHERE object_schema = 'portfolio' 
  AND object_name LIKE 'chat_%' 
ORDER BY count_read DESC;
```

## 🧪 스트레스 테스트 시나리오

### 1. **메시지 폭주 테스트**
```bash
# 100명이 동시에 메시지 전송
for i in {1..100}; do
  curl -X POST http://localhost:8080/api/chat/rooms/1500/messages \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer ${token}" \
    -d '{"content": "스트레스 테스트 메시지 '${i}'"}' &
done
```

### 2. **읽음 상태 업데이트 폭주**
```bash
# 여러 사용자가 동시에 메시지 읽음 처리
for room_id in {1500..1600}; do
  curl -X PUT http://localhost:8080/api/chat/rooms/${room_id}/read \
    -H "Authorization: Bearer ${token}" &
done
```

### 3. **채팅방 목록 조회 폭주**
```bash
# 페이지네이션 스트레스 테스트
for page in {0..100}; do
  curl "http://localhost:8080/api/chat/rooms?page=${page}&size=20" \
    -H "Authorization: Bearer ${token}" &
done
```

## 🎉 결과 분석

### **성공 기준**
- ✅ 모든 쿼리 응답시간 < 500ms
- ✅ 동시 접속 1000명 처리 가능
- ✅ 메모리 사용량 < 2GB
- ✅ CPU 사용률 < 80%

### **최적화 우선순위**
1. **읽음 상태 관리** - 가장 복잡하고 부하가 큰 기능
2. **메시지 조회** - 가장 빈번한 작업  
3. **채팅방 목록** - 사용자 경험에 직접적 영향
4. **실시간 알림** - 웹소켓 성능

이 가이드로 채팅 시스템의 모든 성능 병목점을 찾아 최적화할 수 있습니다! 🚀
