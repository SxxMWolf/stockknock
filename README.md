# StockKnock - AI 기반 통합 주식 분석 플랫폼

국내·해외 주식 투자자를 위한 AI 기반 주식 분석 플랫폼입니다.

## 📖 문서

**상세 문서**: [PROJECT_DOCUMENTATION.md](./PROJECT_DOCUMENTATION.md)를 참고하세요.

프로젝트의 전체 구현 내용, 데이터베이스 설계, API 엔드포인트, 외부 API 연동 가이드 등이 포함되어 있습니다.

## 🚀 빠른 시작

### 사전 요구사항
- Java 17 이상
- Node.js 18 이상
- PostgreSQL 12 이상

### 실행 방법

1. **데이터베이스 설정**
```sql
CREATE DATABASE stockknockdb;
CREATE USER sxxm WITH PASSWORD 'sxxmpass';
GRANT ALL PRIVILEGES ON DATABASE stockknockdb TO sxxm;
```

2. **Backend 실행**
```bash
cd knockBE
./gradlew bootRun
```

3. **Frontend 실행**
```bash
cd knockFE
npm install
npm run dev
```

## 주요 기능

- 📈 실시간 주가 정보 및 자동 업데이트
- 🤖 AI 뉴스 분석 (GPT-4)
- 📊 포트폴리오 관리 및 AI 분석
- 🔔 가격 알림 (목표가/손절가/변동률)
- 💬 AI 채팅 (문맥 유지)
- 📰 뉴스 자동 수집 및 종목 연관 분석
- ⭐ 관심 종목 관리

## 기술 스택

**Backend**: Java 17, Spring Boot 4.0, PostgreSQL, JWT, OpenAI GPT-4  
**Frontend**: React 19, TypeScript, Vite, TanStack Query

자세한 내용은 [PROJECT_DOCUMENTATION.md](./PROJECT_DOCUMENTATION.md)를 참고하세요.

