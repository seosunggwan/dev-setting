# 🚀 Bastion + 다중 EC2 배포 가이드

Private Subnet에 있는 다중 EC2 인스턴스에 Docker Compose를 활용한 배포 방식입니다.

## 🏗️ 아키텍처

```
┌─────────────┐    ┌──────────────┐    ┌─────────────┐
│   GitHub    │───▶│ GitHub       │───▶│   AWS ECR   │
│ Repository  │    │ Actions      │    │ (Registry)  │
└─────────────┘    └──────────────┘    └─────────────┘
                           │                    │
                           ▼                    │
                   ┌──────────────┐             │
                   │  Docker 빌드  │             │
                   │  ECR 푸시    │             │
                   └──────────────┘             │
                           │                    │
                           ▼                    │
          ┌─────────────────────────────────────┼──────┐
          │                VPC                  │      │
          │  ┌─────────────┐  Public Subnet     │      │
          │  │   Bastion   │◄──────────────────┐│      │
          │  │    Host     │                   ││      │
          │  └─────────────┘                   ││      │
          │         │                          ││      │
          │         │ Private Subnet           ││      │
          │  ┌──────▼──────┐  ┌───────────────┐││      │
          │  │    EC2-1    │  │     EC2-2     │││      │
          │  │ spring-app  │  │  spring-app   │││      │
          │  │   redis     │  │    redis      │││◄─────┘
          │  └─────────────┘  └───────────────┘││
          └─────────────────────────────────────┘│
                                                │
          ┌─────────────────────────────────────┘
          │                RDS
          │  ┌─────────────────────────────────┐
          │  │           MySQL DB              │
          │  └─────────────────────────────────┘
          └─────────────────────────────────────
```

## 📋 1단계: AWS 인프라 준비

### 필수 리소스
- **VPC**: Public/Private Subnet 구성
- **Bastion Host**: Public Subnet에 위치
- **EC2 Instances**: Private Subnet에 2개 이상
- **ECR Repository**: portfolio-backend
- **RDS**: MySQL 데이터베이스
- **Security Groups**: 적절한 네트워크 규칙

### Security Group 설정
```bash
# Bastion Host Security Group
- SSH (22): 0.0.0.0/0 (또는 특정 IP)

# EC2 Security Group
- SSH (22): Bastion Host Security Group
- HTTP (8080): ALB Security Group (또는 Bastion)

# RDS Security Group
- MySQL (3306): EC2 Security Group
```

## 📋 2단계: EC2 초기 설정

### 1. 파일 배포
```bash
# 로컬에서 EC2에 필요한 파일들 전송
cd ec2-setup
chmod +x deploy-files.sh

# EC2-1에 파일 배포
./deploy-files.sh your-bastion-host 10.0.1.100 ~/.ssh/ec2-key.pem

# EC2-2에 파일 배포
./deploy-files.sh your-bastion-host 10.0.1.101 ~/.ssh/ec2-key.pem
```

### 2. EC2 접속 및 설정
```bash
# EC2-1 접속
ssh -i ~/.ssh/ec2-key.pem -o ProxyJump=ubuntu@your-bastion-host ubuntu@10.0.1.100

# 초기 설정 실행
chmod +x ~/setup-ec2.sh
~/setup-ec2.sh

# Docker 그룹 적용을 위해 재접속
exit
ssh -i ~/.ssh/ec2-key.pem -o ProxyJump=ubuntu@your-bastion-host ubuntu@10.0.1.100
```

### 3. 환경 변수 설정
```bash
cd ~/app
cp .env.template .env
vi .env  # 실제 값으로 수정
```

### 4. AWS CLI 설정
```bash
aws configure
# Access Key ID: [IAM User의 Access Key]
# Secret Access Key: [IAM User의 Secret Key]
# Default region: ap-northeast-2
# Default output format: json
```

### 5. ECR 로그인 테스트
```bash
aws ecr get-login-password --region ap-northeast-2 | \
    docker login --username AWS --password-stdin YOUR_ECR_REGISTRY
```

### 6. 배포 테스트
```bash
# 이미지 pull 테스트
docker-compose pull

# 컨테이너 시작
docker-compose up -d

# 상태 확인
docker ps
docker logs spring-app

# 헬스체크
curl localhost:8080/api/actuator/health
```

**⚠️ EC2-2에도 동일하게 설정**

## 📋 3단계: GitHub Secrets 설정

Repository → Settings → Secrets에서 다음 추가:

### 필수 설정
```
AWS_ACCESS_KEY_ID=your-access-key
AWS_SECRET_ACCESS_KEY=your-secret-key
EC2_SSH_KEY=-----BEGIN RSA PRIVATE KEY-----...
BASTION_HOST=your-bastion-public-ip-or-domain
EC2_HOST_1=10.0.1.100
EC2_HOST_2=10.0.1.101
```

## 📋 4단계: 자동 배포

### 배포 실행
```bash
# main 브랜치에 push하면 자동 배포
git add .
git commit -m "feat: 다중 EC2 배포 설정"
git push origin main
```

### 배포 과정
1. **코드 체크아웃** ✅
2. **Java & Gradle 빌드** ✅
3. **Docker 이미지 빌드** ✅
4. **ECR에 푸시** ✅
5. **EC2-1에 배포** ✅
6. **EC2-2에 배포** ✅

## 📊 모니터링 및 관리

### 1. 상태 확인
```bash
# EC2 접속
ssh -i ~/.ssh/ec2-key.pem -o ProxyJump=ubuntu@bastion-host ubuntu@ec2-host

# 컨테이너 상태
docker ps

# 애플리케이션 로그
docker logs -f spring-app

# Redis 로그
docker logs redis-server
```

### 2. 수동 배포
```bash
cd ~/app

# 서비스 중지
docker-compose down

# 최신 이미지 pull
docker-compose pull

# 서비스 시작
docker-compose up -d
```

### 3. 로그 모니터링
```bash
# 실시간 로그
docker-compose logs -f

# 특정 서비스 로그
docker-compose logs -f app
docker-compose logs -f redis

# 에러 로그만 확인
docker logs spring-app 2>&1 | grep -i error
```

## 🔧 트러블슈팅

### 1. Bastion 접속 실패
```bash
# SSH 키 권한 확인
chmod 600 ~/.ssh/ec2-key.pem

# Bastion Security Group 확인 (22 포트)
# Bastion Host의 Public IP 확인
```

### 2. ECR 로그인 실패
```bash
# AWS CLI 설정 확인
aws configure list

# IAM 권한 확인 (ECR 관련 정책)
aws ecr describe-repositories --repository-names portfolio-backend
```

### 3. 컨테이너 시작 실패
```bash
# .env 파일 확인
cat ~/app/.env

# 이미지 존재 확인
docker images | grep portfolio-backend

# 네트워크 확인
docker network ls
```

### 4. 데이터베이스 연결 실패
```bash
# RDS Security Group 확인
# RDS 엔드포인트 및 포트 확인
# EC2에서 RDS 연결 테스트
telnet your-rds-endpoint 3306
```

## 📈 확장성 고려사항

### 1. Load Balancer 추가
```bash
# Application Load Balancer 생성
# Target Group에 EC2 인스턴스들 등록
# Health Check 설정: /api/actuator/health
```

### 2. Auto Scaling
```bash
# Launch Template 생성
# Auto Scaling Group 설정
# CloudWatch 메트릭 기반 스케일링
```

### 3. 로그 중앙화
```bash
# CloudWatch Logs Agent 설치
# ELK Stack 구성
# Fluentd/Fluent Bit 설정
```

## 🎯 체크리스트

### EC2 설정 체크리스트
- [ ] Docker & Docker Compose 설치됨
- [ ] AWS CLI 설정됨
- [ ] ECR 로그인 성공
- [ ] docker-compose.yml 배치됨
- [ ] .env 파일 설정됨
- [ ] 수동 배포 테스트 성공

### GitHub Actions 체크리스트
- [ ] 모든 Secrets 설정됨
- [ ] ECR Repository 존재함
- [ ] Bastion Host 접속 가능
- [ ] 워크플로우 실행 성공
- [ ] 양쪽 EC2에 배포 성공

### 운영 체크리스트
- [ ] 헬스체크 응답 정상
- [ ] 로그 모니터링 설정
- [ ] 백업 전략 수립
- [ ] 장애 대응 절차 문서화

---

**💡 팁**: 처음에는 EC2 1대로 테스트해보고, 정상 동작 확인 후 다중 EC2로 확장하는 것을 권장합니다! 