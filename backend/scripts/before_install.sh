#!/bin/bash

# 색상 정의
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_info "Before Install 단계 시작"

# Docker 설치 확인 및 설치
if ! command -v docker &> /dev/null; then
    log_info "Docker 설치 중..."
    sudo apt-get update
    sudo apt-get install -y docker.io
    sudo systemctl start docker
    sudo systemctl enable docker
    sudo usermod -aG docker ubuntu
    log_success "Docker 설치 완료"
else
    log_info "Docker가 이미 설치되어 있습니다."
fi

# AWS CLI 설치 확인 및 설치
if ! command -v aws &> /dev/null; then
    log_info "AWS CLI 설치 중..."
    curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
    unzip -q awscliv2.zip
    sudo ./aws/install
    rm -rf awscliv2.zip aws/
    log_success "AWS CLI 설치 완료"
else
    log_info "AWS CLI가 이미 설치되어 있습니다."
fi

# ECR 로그인 (환경변수가 설정되어 있다면)
if [ ! -z "$ECR_REGISTRY" ]; then
    log_info "ECR 로그인 중..."
    aws ecr get-login-password --region ap-northeast-2 | docker login --username AWS --password-stdin $ECR_REGISTRY
    log_success "ECR 로그인 완료"
fi

log_success "Before Install 단계 완료"