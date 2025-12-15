#!/bin/bash

# FastAPI 실행 스크립트
# 사용법: ./run.sh

cd "$(dirname "$0")"

# 가상환경이 없으면 생성
if [ ! -d "venv" ]; then
    echo "가상환경이 없습니다. 생성 중..."
    python3 -m venv venv
    echo "의존성 설치 중..."
    source venv/bin/activate
    pip install --upgrade pip
    pip install -r requirements.txt
    echo "설치 완료!"
fi

# 가상환경 활성화 및 실행
source venv/bin/activate
echo "FastAPI 서버 시작 중..."
uvicorn app.main:app --reload
