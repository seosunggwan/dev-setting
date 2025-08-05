#!/bin/bash

# 색상 정의
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_info "Application Stop 단계 시작"

# 기존 컨테이너 중지 및 제거
CONTAINER_NAME="instagram-server"

if [ "$(docker ps -q -f name=$CONTAINER_NAME)" ]; then
    log_info "실행 중인 컨테이너 '$CONTAINER_NAME' 중지 중..."
    docker stop $CONTAINER_NAME
    log_success "컨테이너 중지 완료"
else
    log_warning "실행 중인 '$CONTAINER_NAME' 컨테이너가 없습니다."
fi

if [ "$(docker ps -aq -f name=$CONTAINER_NAME)" ]; then
    log_info "기존 컨테이너 '$CONTAINER_NAME' 제거 중..."
    docker rm $CONTAINER_NAME
    log_success "컨테이너 제거 완료"
else
    log_warning "제거할 '$CONTAINER_NAME' 컨테이너가 없습니다."
fi

# 사용하지 않는 Docker 이미지 정리
log_info "사용하지 않는 Docker 이미지 정리 중..."
docker image prune -f
log_success "Docker 이미지 정리 완료"

log_success "Application Stop 단계 완료"