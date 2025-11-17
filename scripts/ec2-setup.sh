#!/bin/bash

# ============================================
# EC2 인스턴스 초기 설정 스크립트
# ============================================
# 이 스크립트는 EC2 인스턴스에서 한 번만 실행하면 됩니다.

set -e  # 에러 발생 시 스크립트 중단

echo "========================================"
echo "EC2 초기 설정 시작"
echo "========================================"

# 패키지 업데이트
echo "📦 패키지 업데이트 중..."
sudo yum update -y

# Docker 설치
echo "🐳 Docker 설치 중..."
if ! command -v docker &> /dev/null; then
    sudo yum install -y docker
    sudo systemctl start docker
    sudo systemctl enable docker
    sudo usermod -aG docker $USER
    echo "✅ Docker 설치 완료"
else
    echo "ℹ️  Docker가 이미 설치되어 있습니다."
fi

# Docker Compose 설치
echo "🐳 Docker Compose 설치 중..."
if ! command -v docker-compose &> /dev/null; then
    sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    sudo chmod +x /usr/local/bin/docker-compose
    echo "✅ Docker Compose 설치 완료"
else
    echo "ℹ️  Docker Compose가 이미 설치되어 있습니다."
fi

# AWS CLI 설치
echo "☁️  AWS CLI 설치 중..."
if ! command -v aws &> /dev/null; then
    curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
    unzip awscliv2.zip
    sudo ./aws/install
    rm -rf aws awscliv2.zip
    echo "✅ AWS CLI 설치 완료"
else
    echo "ℹ️  AWS CLI가 이미 설치되어 있습니다."
fi

# Git 설치
echo "📝 Git 설치 중..."
if ! command -v git &> /dev/null; then
    sudo yum install -y git
    echo "✅ Git 설치 완료"
else
    echo "ℹ️  Git이 이미 설치되어 있습니다."
fi

# 프로젝트 디렉토리 설정
echo "📁 프로젝트 디렉토리 설정 중..."
cd ~
if [ ! -d "webmail" ]; then
    echo "GitHub 저장소 URL을 입력하세요:"
    read REPO_URL
    git clone $REPO_URL webmail
    cd webmail
else
    echo "ℹ️  webmail 디렉토리가 이미 존재합니다."
    cd webmail
fi

# 환경변수 파일 생성
echo "🔐 환경변수 파일 생성 중..."
if [ ! -f ".env" ]; then
    cp .env.example .env
    echo "⚠️  .env 파일을 생성했습니다. 반드시 편집하여 실제 값을 입력하세요!"
    echo "   명령어: vim ~/.webmail/.env"
else
    echo "ℹ️  .env 파일이 이미 존재합니다."
fi

# AWS 자격증명 설정
echo "☁️  AWS 자격증명 설정..."
echo "AWS CLI를 설정합니다. AWS Access Key ID와 Secret Access Key가 필요합니다."
echo "설정하시겠습니까? (y/n)"
read -r SETUP_AWS

if [ "$SETUP_AWS" = "y" ]; then
    aws configure
else
    echo "⚠️  나중에 'aws configure' 명령어로 설정하세요."
fi

echo ""
echo "========================================"
echo "✅ EC2 초기 설정 완료!"
echo "========================================"
echo ""
echo "다음 단계:"
echo "1. 로그아웃 후 다시 로그인 (Docker 그룹 적용)"
echo "2. .env 파일 편집: vim ~/webmail/.env"
echo "3. AWS CLI 설정 (아직 안했다면): aws configure"
echo "4. ECR에서 이미지 pull 테스트:"
echo "   aws ecr get-login-password --region ap-northeast-2 | docker login --username AWS --password-stdin <YOUR_ECR_URI>"
echo ""
echo "이제 GitHub에서 push하면 자동으로 배포됩니다!"
echo ""
