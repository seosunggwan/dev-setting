# 💬 다모아 - 실시간 소통 커뮤니티

1인 개발로 프론트·백엔드부터 배포까지 모두 구현한 풀스택 사이드 프로젝트입니다.  
게시판, 실시간 채팅, 상품 주문, OAuth 로그인 등 다양한 기능을 통합 구현했습니다.

---

## 🛠️ 기술 스택

### 백엔드
- Java 17, Spring Boot 3.3
- Spring Security, OAuth2 Client, JWT
- Spring Data JPA, MySQL, Redis
- WebSocket (STOMP/SockJS)
- AWS (EC2, RDS, S3, CodeDeploy, ECR)
- Docker, GitHub Actions

### 프론트엔드
- React 19, Vite
- Material UI, React Router, Axios
- SockJS, React Cookie

---

## 🌐 아키텍처 구성

- ALB + EC2(Spring Boot)
- EC2에 Redis 설치 운영
- RDS (Multi-AZ 구성)
- VPC, 서브넷, NAT Gateway 등 AWS 네트워크 구성
- S3 + CloudFront 정적 웹 배포

---

## 🚀 주요 기능

### 💬 채팅 시스템
- 1:1 / 그룹 채팅 (WebSocket + Redis Pub/Sub)
- 메시지 읽음 처리 및 실시간 알림
- 동시 접속 관리 및 세션 최적화

### 📝 게시판 시스템
- 게시글/댓글 작성, 수정, 삭제
- 2-depth 댓글 구조
- 인기글 별도 테이블 및 랭킹 점수 기반 정렬

### 🛒 상품 및 주문 시스템
- 상품 등록/조회/수정/삭제
- 주문 처리 및 상태 관리
- 관리자용 검색 및 필터 기능

### 👤 사용자 인증 및 관리
- 일반 로그인 + 소셜 로그인 (카카오, 네이버)
- JWT 인증 및 Refresh Token Redis 캐싱
- 프로필 정보 수정, 이미지 업로드

---

## ⚙️ 성능 최적화 사례

### ✅ 인기글 캐싱
- 실시간 정렬 대신 별도 통계 테이블 도입
- 복합 인덱스 설계로 filesort 제거
- 평균 응답 시간 1.8초 → 80ms 개선

### ✅ 인증 성능 개선
- Redis TTL 캐시 도입으로 DB 의존 제거
- 인증 API p95 응답 시간 2100ms → 620ms
- TPS 80% 향상, DB 부하 감소

### ✅ 실시간 채팅 최적화
- 비동기 메시지 처리 구조
- Redis Pub/Sub 도입으로 확장성 개선
- 채팅 응답 시간 1800ms → 510ms

---

## 🧪 테스트 및 배포

- GitHub Actions + CodeDeploy + ECR 기반 CI/CD
- Backend: EC2에 Docker Compose로 배포
- Frontend: S3 정적 웹 배포 + CloudFront 캐시 무효화
- Redis 분산락 및 TTL Jitter로 캐시 동시 만료 방지

---

## 📦 프로젝트 구조 및 링크

- GitHub Repo: [https://github.com/seosunggwan/demo3/tree/webrtc](https://github.com/seosunggwan/demo3/tree/webrtc)
