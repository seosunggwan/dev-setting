#!/bin/bash

# 스크립트 설정
set -e  # 오류 발생 시 스크립트 중단

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 로그 함수
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 환경 변수 체크
check_env_vars() {
    log_info "환경 변수 확인 중..."
    
    required_vars=(
        "ECR_REGISTRY"
        "ECR_REPOSITORY" 
        "AWS_REGION"
        "RDS_ENDPOINT"
        "RDS_PASSWORD"
        "JWT_SECRET"
        "AWS_ACCESS_KEY_ID"
        "AWS_SECRET_ACCESS_KEY"
        "AWS_S3_BUCKET"
    )
    
    for var in "${required_vars[@]}"; do
        if [ -z "${!var}" ]; then
            log_error "필수 환경변수 $var 가 설정되지 않았습니다."
            exit 1
        fi
    done
    
    log_success "모든 필수 환경변수가 설정되었습니다."
}

# 필수 소프트웨어 설치
install_dependencies() {
    log_info "의존성 소프트웨어 설치 확인 중..."
    
    # AWS CLI 설치 확인
    if ! command -v aws &> /dev/null; then
        log_warning "AWS CLI가 설치되지 않음. 설치 중..."
        curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
        unzip awscliv2.zip
        sudo ./aws/install
        rm -rf aws awscliv2.zip
        log_success "AWS CLI 설치 완료"
    else
        log_success "AWS CLI가 이미 설치되어 있습니다."
    fi
    
    # Docker 설치 확인
    if ! command -v docker &> /dev/null; then
        log_warning "Docker가 설치되지 않음. 설치 중..."
        sudo apt-get update
        sudo apt-get install -y \
            ca-certificates \
            curl \
            gnupg \
            lsb-release
        
        curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg
        echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
        
        sudo apt-get update
        sudo apt-get install -y docker-ce docker-ce-cli containerd.io
        sudo systemctl start docker
        sudo systemctl enable docker
        sudo usermod -aG docker $USER
        log_success "Docker 설치 완료"
    else
        log_success "Docker가 이미 설치되어 있습니다."
    fi
    
    # Docker Compose 설치 확인
    if ! command -v docker-compose &> /dev/null; then
        log_warning "Docker Compose가 설치되지 않음. 설치 중..."
        sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
        sudo chmod +x /usr/local/bin/docker-compose
        log_success "Docker Compose 설치 완료"
    else
        log_success "Docker Compose가 이미 설치되어 있습니다."
    fi
}

# ECR 로그인
ecr_login() {
    log_info "ECR에 로그인 중..."
    aws ecr get-login-password --region ${AWS_REGION} | sudo docker login --username AWS --password-stdin ${ECR_REGISTRY}
    log_success "ECR 로그인 완료"
}

# 기존 컨테이너 정리
cleanup_containers() {
    log_info "기존 컨테이너 정리 중..."
    
    # 실행 중인 컨테이너 중지
    if sudo docker ps -q --filter "name=spring-app" | grep -q .; then
        log_info "기존 spring-app 컨테이너를 중지합니다..."
        sudo docker stop spring-app
        log_success "컨테이너 중지 완료"
    fi
    
    # 컨테이너 제거
    if sudo docker ps -aq --filter "name=spring-app" | grep -q .; then
        log_info "기존 spring-app 컨테이너를 제거합니다..."
        sudo docker rm spring-app
        log_success "컨테이너 제거 완료"
    fi
    
    # 사용하지 않는 이미지 정리
    log_info "사용하지 않는 Docker 이미지 정리 중..."
    sudo docker image prune -f
    log_success "이미지 정리 완료"
}

# 새 이미지 Pull
pull_image() {
    local image_uri="$1"
    log_info "새 이미지를 다운로드 중: $image_uri"
    sudo docker pull "$image_uri"
    log_success "이미지 다운로드 완료"
}

# 로그 디렉토리 생성
create_log_directory() {
    log_info "로그 디렉토리 생성 중..."
    sudo mkdir -p /var/log/portfolio-backend
    sudo chown $USER:$USER /var/log/portfolio-backend
    log_success "로그 디렉토리 생성 완료"
}

# 애플리케이션 배포
deploy_application() {
    local image_uri="$1"
    
    log_info "애플리케이션 배포 중..."
    
    # 로그 디렉토리 생성
    create_log_directory
    
    # 컨테이너 실행
    sudo docker run -d \
        --name spring-app \
        --restart unless-stopped \
        -p 8080:8080 \
        -e SPRING_PROFILES_ACTIVE=prod \
        -e RDS_ENDPOINT="${RDS_ENDPOINT}" \
        -e RDS_USERNAME="${RDS_USERNAME:-portfolio_user}" \
        -e RDS_PASSWORD="${RDS_PASSWORD}" \
        -e JWT_SECRET="${JWT_SECRET}" \
        -e AWS_ACCESS_KEY="${AWS_ACCESS_KEY_ID}" \
        -e AWS_SECRET_KEY="${AWS_SECRET_ACCESS_KEY}" \
        -e AWS_S3_BUCKET="${AWS_S3_BUCKET}" \
        -e AWS_REGION="${AWS_REGION}" \
        -e SPRING_DATA_REDIS_HOST="${SPRING_DATA_REDIS_HOST:-redis}" \
        -e SPRING_DATA_REDIS_PORT="${SPRING_DATA_REDIS_PORT:-6379}" \
        -e SPRING_DATA_REDIS_PASSWORD="${SPRING_DATA_REDIS_PASSWORD:-1234}" \
        -e NAVER_CLIENT_ID="${NAVER_CLIENT_ID:-}" \
        -e NAVER_CLIENT_SECRET="${NAVER_CLIENT_SECRET:-}" \
        -e GOOGLE_CLIENT_ID="${GOOGLE_CLIENT_ID:-}" \
        -e GOOGLE_CLIENT_SECRET="${GOOGLE_CLIENT_SECRET:-}" \
        -e GITHUB_CLIENT_ID="${GITHUB_CLIENT_ID:-}" \
        -e GITHUB_CLIENT_SECRET="${GITHUB_CLIENT_SECRET:-}" \
        -e CORS_ALLOWED_ORIGINS="${CORS_ALLOWED_ORIGINS:-http://localhost:3000}" \
        -e DOMAIN_NAME="${DOMAIN_NAME:-localhost}" \
        "$image_uri"
    
    log_success "애플리케이션 배포 완료"
}

# 헬스체크
health_check() {
    log_info "애플리케이션 헬스체크 중..."
    
    local max_attempts=30
    local attempt=1
    
    while [ $attempt -le $max_attempts ]; do
        if curl -f http://localhost:8080/api/actuator/health &> /dev/null; then
            log_success "애플리케이션이 정상적으로 시작되었습니다!"
            return 0
        fi
        
        log_info "헬스체크 시도 $attempt/$max_attempts..."
        sleep 10
        ((attempt++))
    done
    
    log_error "애플리케이션 헬스체크 실패"
    return 1
}

# 배포 상태 확인
check_deployment_status() {
    log_info "배포 상태 확인 중..."
    
    # 컨테이너 상태 확인
    if sudo docker ps | grep -q spring-app; then
        log_success "컨테이너가 정상적으로 실행 중입니다."
        sudo docker ps | grep spring-app
        
        # 컨테이너 로그 마지막 몇 줄 출력
        log_info "최근 애플리케이션 로그:"
        sudo docker logs --tail 20 spring-app
        
        return 0
    else
        log_error "컨테이너가 실행되지 않았습니다."
        log_info "컨테이너 로그:"
        sudo docker logs spring-app
        return 1
    fi
}

# 메인 배포 함수
main() {
    local image_tag="${1:-latest}"
    local image_uri="${ECR_REGISTRY}/${ECR_REPOSITORY}:${image_tag}"
    
    log_info "=== Portfolio Backend 배포 시작 ==="
    log_info "이미지: $image_uri"
    
    # 환경 변수 체크
    check_env_vars
    
    # 의존성 설치
    install_dependencies
    
    # ECR 로그인
    ecr_login
    
    # 기존 컨테이너 정리
    cleanup_containers
    
    # 새 이미지 Pull
    pull_image "$image_uri"
    
    # 애플리케이션 배포
    deploy_application "$image_uri"
    
    # 헬스체크
    if health_check; then
        # 배포 상태 확인
        if check_deployment_status; then
            log_success "=== 배포가 성공적으로 완료되었습니다! ==="
            log_info "애플리케이션 URL: http://$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4):8080/api"
            log_info "헬스체크 URL: http://$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4):8080/api/actuator/health"
        else
            log_error "배포 상태 확인 실패"
            exit 1
        fi
    else
        log_error "헬스체크 실패"
        exit 1
    fi
}

# 스크립트 실행
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi 