#!/bin/bash

# EC2 인스턴스 초기 설정 스크립트
set -e

echo "🚀 EC2 초기 설정을 시작합니다..."

# 시스템 업데이트
echo "[1] 시스템 업데이트"
sudo apt-get update

# Docker 설치
echo "[2] Docker 설치"
if ! command -v docker &> /dev/null; then
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
    sudo usermod -aG docker ubuntu
    echo "✅ Docker 설치 완료"
else
    echo "✅ Docker가 이미 설치되어 있습니다"
fi

# Docker Compose 설치
echo "[3] Docker Compose 설치"
if ! command -v docker-compose &> /dev/null; then
    sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    sudo chmod +x /usr/local/bin/docker-compose
    echo "✅ Docker Compose 설치 완료"
else
    echo "✅ Docker Compose가 이미 설치되어 있습니다"
fi

# AWS CLI 설치
echo "[4] AWS CLI 설치"
if ! command -v aws &> /dev/null; then
    curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
    unzip awscliv2.zip
    sudo ./aws/install
    rm -rf aws awscliv2.zip
    echo "✅ AWS CLI 설치 완료"
else
    echo "✅ AWS CLI가 이미 설치되어 있습니다"
fi

# 작업 디렉토리 생성
echo "[5] 작업 디렉토리 설정"
mkdir -p ~/app
cd ~/app

# docker-compose.yml 파일이 없으면 생성
if [ ! -f "docker-compose.yml" ]; then
    echo "⚠️  docker-compose.yml 파일이 없습니다. 템플릿을 복사해주세요."
fi

# .env 파일이 없으면 템플릿 생성
if [ ! -f ".env" ]; then
    echo "⚠️  .env 파일이 없습니다. .env.template을 참고하여 생성해주세요."
fi

echo ""
echo "🎉 EC2 초기 설정이 완료되었습니다!"
echo ""
echo "📋 다음 단계:"
echo "1. ~/app 디렉토리에 docker-compose.yml 파일 배치"
echo "2. .env 파일 생성 및 환경 변수 설정"
echo "3. AWS CLI 자격 증명 설정: aws configure"
echo "4. ECR 로그인 테스트: aws ecr get-login-password --region ap-northeast-2"
echo ""
echo "💡 참고: Docker 그룹 권한 적용을 위해 로그아웃 후 다시 로그인하세요." 