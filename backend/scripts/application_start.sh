#!/bin/bash

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_info "Application Start 단계 시작"

# 환경변수 설정 (CodeDeploy에서 전달받을 수 있도록)
ECR_REGISTRY="202533515601.dkr.ecr.ap-northeast-2.amazonaws.com"
CONTAINER_NAME="instagram-server"
IMAGE_TAG="latest"

# ECR에서 최신 이미지 Pull
log_info "ECR에서 최신 이미지 Pull 중..."
if ! docker pull $ECR_REGISTRY/instagram-server:$IMAGE_TAG; then
    log_error "Docker 이미지 Pull 실패"
    exit 1
fi
log_success "Docker 이미지 Pull 완료"

# 컨테이너 실행
log_info "새 컨테이너 시작 중..."
docker run -d \
    --name $CONTAINER_NAME \
    -p 8080:8080 \
    --restart unless-stopped \
    $ECR_REGISTRY/instagram-server:$IMAGE_TAG

if [ $? -eq 0 ]; then
    log_success "컨테이너 시작 완료"
else
    log_error "컨테이너 시작 실패"
    exit 1
fi

# 컨테이너 상태 확인
log_info "컨테이너 상태 확인 중..."
sleep 5

if [ "$(docker ps -q -f name=$CONTAINER_NAME)" ]; then
    log_success "컨테이너가 정상적으로 실행 중입니다."
    docker ps --filter name=$CONTAINER_NAME
else
    log_error "컨테이너 실행에 문제가 있습니다."
    docker logs $CONTAINER_NAME
    exit 1
fi

log_success "Application Start 단계 완료"