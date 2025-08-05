#!/bin/bash

# EC2 인스턴스에 필요한 파일들을 배포하는 스크립트
set -e

# 사용법 체크
if [ $# -lt 2 ]; then
    echo "사용법: $0 <BASTION_HOST> <EC2_HOST> [EC2_SSH_KEY_PATH]"
    echo "예시: $0 bastion.example.com 10.0.1.100 ~/.ssh/ec2-key.pem"
    exit 1
fi

BASTION_HOST=$1
EC2_HOST=$2
SSH_KEY=${3:-~/.ssh/id_rsa}

echo "🚀 EC2에 파일 배포를 시작합니다..."
echo "📡 Bastion Host: $BASTION_HOST"
echo "🖥️  EC2 Host: $EC2_HOST"
echo "🔑 SSH Key: $SSH_KEY"

# SSH 키 권한 확인
chmod 600 "$SSH_KEY"

# EC2에 디렉토리 생성
echo "[1] EC2에 디렉토리 생성"
ssh -i "$SSH_KEY" -o ProxyJump=ubuntu@"$BASTION_HOST" ubuntu@"$EC2_HOST" \
    "mkdir -p ~/app"

# docker-compose.yml 파일 전송
echo "[2] docker-compose.yml 파일 전송"
scp -i "$SSH_KEY" -o ProxyJump=ubuntu@"$BASTION_HOST" \
    docker-compose.yml ubuntu@"$EC2_HOST":/home/ubuntu/app/

# .env 템플릿 파일 전송
echo "[3] .env.template 파일 전송"
scp -i "$SSH_KEY" -o ProxyJump=ubuntu@"$BASTION_HOST" \
    .env.template ubuntu@"$EC2_HOST":/home/ubuntu/app/

# 초기 설정 스크립트 전송
echo "[4] 설정 스크립트 전송"
scp -i "$SSH_KEY" -o ProxyJump=ubuntu@"$BASTION_HOST" \
    setup-ec2.sh ubuntu@"$EC2_HOST":/home/ubuntu/

echo ""
echo "✅ 파일 전송이 완료되었습니다!"
echo ""
echo "📋 다음 단계:"
echo "1. EC2에 접속하여 초기 설정 실행:"
echo "   ssh -i $SSH_KEY -o ProxyJump=ubuntu@$BASTION_HOST ubuntu@$EC2_HOST"
echo "   chmod +x ~/setup-ec2.sh && ~/setup-ec2.sh"
echo ""
echo "2. .env 파일 생성:"
echo "   cd ~/app"
echo "   cp .env.template .env"
echo "   vi .env  # 실제 값으로 수정"
echo ""
echo "3. AWS CLI 설정:"
echo "   aws configure"
echo ""
echo "4. 배포 테스트:"
echo "   docker-compose pull"
echo "   docker-compose up -d" 