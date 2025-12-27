#!/bin/bash

echo "========================================="
echo "  StocKKnock 빠른 시작 스크립트"
echo "========================================="
echo ""

# 색상 정의
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# PostgreSQL 확인
echo -e "${YELLOW}[1/5] PostgreSQL 연결 확인 중...${NC}"
if PGPASSWORD=sxxmpass psql -U sxxm -d stockknockDB -h localhost -c "SELECT 1;" > /dev/null 2>&1; then
    echo -e "${GREEN}✓ PostgreSQL 연결 성공${NC}"
else
    echo -e "${RED}✗ PostgreSQL 연결 실패${NC}"
    echo "데이터베이스를 먼저 생성해주세요:"
    echo "  psql postgres"
    echo "  CREATE DATABASE stockknockDB;"
    echo "  CREATE USER sxxm WITH PASSWORD 'sxxmpass';"
    echo "  GRANT ALL PRIVILEGES ON DATABASE stockknockDB TO sxxm;"
    echo "자세한 내용은 POSTGRESQL_SETUP.md를 참고하세요."
    exit 1
fi

# 백엔드 빌드
echo -e "${YELLOW}[2/5] 백엔드 빌드 중...${NC}"
cd knockBE
chmod +x gradlew 2>/dev/null
if ./gradlew clean build -x test > /dev/null 2>&1; then
    echo -e "${GREEN}✓ 백엔드 빌드 성공${NC}"
else
    echo -e "${RED}✗ 백엔드 빌드 실패${NC}"
    echo "상세 로그를 확인하려면: cd knockBE && ./gradlew clean build"
    exit 1
fi
cd ..

# 프론트엔드 의존성 확인
echo -e "${YELLOW}[3/5] 프론트엔드 의존성 확인 중...${NC}"
cd knockFE
if [ ! -d "node_modules" ]; then
    echo "의존성 설치 중..."
    npm install > /dev/null 2>&1
fi
echo -e "${GREEN}✓ 프론트엔드 준비 완료${NC}"
cd ..

# FastAPI 의존성 확인
echo -e "${YELLOW}[4/5] FastAPI 의존성 확인 중...${NC}"
cd knockAI
if [ ! -d "venv" ]; then
    echo "가상 환경 생성 중..."
    python3 -m venv venv > /dev/null 2>&1
fi
if [ ! -f "venv/bin/activate" ]; then
    echo -e "${RED}✗ FastAPI 가상 환경 생성 실패${NC}"
    exit 1
fi
echo -e "${GREEN}✓ FastAPI 준비 완료${NC}"
cd ..

echo ""
echo -e "${GREEN}=========================================${NC}"
echo -e "${GREEN}  준비 완료!${NC}"
echo -e "${GREEN}=========================================${NC}"
echo ""
echo "다음 단계:"
echo ""
echo "1. 백엔드 실행 (터미널 1):"
echo "   ${YELLOW}cd knockBE && ./gradlew bootRun${NC}"
echo ""
echo "2. FastAPI 실행 (터미널 2):"
echo "   ${YELLOW}cd knockAI && source venv/bin/activate && uvicorn app.main:app --reload${NC}"
echo ""
echo "3. 프론트엔드 실행 (터미널 3):"
echo "   ${YELLOW}cd knockFE && npm run dev${NC}"
echo ""
echo "4. 브라우저에서 접속:"
echo "   ${YELLOW}http://localhost:5173${NC} (또는 Vite가 표시한 포트)"
echo ""
echo "5. Swagger UI 접속:"
echo "   ${YELLOW}http://localhost:8080/swagger-ui.html${NC}"
echo ""
echo "자세한 내용은 README.md를 참고하세요."

