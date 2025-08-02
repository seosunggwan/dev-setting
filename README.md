---

# 💬 다모아 - 실시간 소통 커뮤니티

1인 개발로 프론트·백엔드부터 배포까지 모두 구현한 풀스택 사이드 프로젝트입니다.
게시판, 실시간 채팅, 상품 주문, OAuth 로그인 등 다양한 기능을 통합 구현했습니다.

<br/>

## 🛠️ 기술 스택

### 백엔드

* Java 17, Spring Boot 3.3
* Spring Security, OAuth2 Client, JWT
* JPA, MySQL, Redis
* WebSocket (STOMP/SockJS)
* AWS S3, EC2, RDS, Redis
* Docker, GitHub Actions, CodeDeploy

### 프론트엔드

* React 19, Vite
* Material UI, React Router, Axios
* SockJS, React Cookie

<br/>

## 🌐 아키텍처

### AWS 인프라 구조

* ALB + EC2(Spring Boot)
* EC2 내 Redis 설치 및 운영
* RDS (Multi-AZ 구성)
* VPC/Subnet, NAT Gateway 구성
* 프론트: S3 + CloudFront 정적 배포

!\[AWS Architecture]\(링크 또는 이미지 경로)

---

## 🚀 주요 기능

### 💬 채팅 시스템

* 1:1 / 그룹 채팅 (WebSocket + Redis Pub/Sub)
* 메시지 읽음 처리, 알림 기능
* 세션 관리 및 동시 접속 확장성 고려

### 📝 게시판 시스템

* 게시글 작성/수정/삭제, 댓글 (2-depth)
* 인기글 별도 저장 테이블로 분리
* 정렬 부하 해소를 위한 점수 기반 랭킹 설계

### 🛒 상품 / 주문 시스템

* 상품 등록/수정/삭제/조회, 이미지 업로드
* 주문 기능 및 상태 변경
* 관리자용 주문 필터링 및 검색 기능

### 👤 사용자 기능

* 일반 로그인 + 소셜 로그인(OAuth2: 카카오/네이버)
* JWT 기반 인증 / Refresh Token Redis 캐싱
* 프로필 관리 및 이미지 업로드

---

## ⚙️ 성능 최적화 사례

### ✅ 인기글 캐싱

* 실시간 계산 대신 통계 테이블 별도 구성
* `selection_date + rank_position` 인덱스 설계
* 응답 시간 1.8초 → 80ms로 개선

### ✅ 인증 시스템 최적화

* Refresh Token Redis TTL 캐싱
* 인증 API 응답 p95: 2100ms → 620ms
* TPS 80% 증가, DB 부하 감소

### ✅ 실시간 채팅 성능 향상

* WebSocket 처리 비동기화 및 세션 분리 관리
* Redis Pub/Sub 도입
* 메시지 응답 시간 1800ms → 510ms

---

## 🧪 테스트 및 배포

* GitHub Actions + CodeDeploy 기반 CI/CD 구축
* Backend: EC2에 docker-compose로 배포
* Frontend: S3 정적 파일 업로드 후 CloudFront 캐시 무효화 처리
* Redis 기반 분산락, TTL Jitter 적용으로 캐시 동시만료 방지

---

## 📦 프로젝트 구조 및 링크
* ERD 및 아키텍처 다이어그램: 리드미 내 이미지 참고

---

필요하면 위에 각 섹션에 이미지 경로나 실제 링크 추가하면 더 완성도 높아질 거야!
더 정리하거나 디자인 요소 넣고 싶으면 말해줘! 😎
