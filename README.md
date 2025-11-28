# StockKnock - AI 기반 통합 주식 분석 웹사이트

국내·해외 주식 투자자를 위한 AI 기반 주식 분석 플랫폼입니다.

## 주요 기능

- 📈 **실시간 주가 정보**: 국내·해외 주요 증시 주식 정보 조회
- 🤖 **AI 뉴스 분석**: GPT를 활용한 뉴스 자동 요약 및 주가 영향 분석
- 📊 **포트폴리오 관리**: 보유 종목 관리 및 손익 실시간 추적
- 🔔 **가격 알림**: 목표가/손절가 도달 시 알림
- 💬 **AI 채팅**: 개인 애널리스트처럼 종목 전망, 산업 동향 분석
- 📰 **뉴스 피드**: 주요 증시 뉴스 자동 수집 및 분석
- 📅 **실적 발표 캘린더**: 기업 실적 발표 일정 및 AI 해석

## 기술 스택

### Backend (knockBE)
- Java 17
- Spring Boot 4.0.0
- Spring Data JPA
- Spring Security (JWT)
- PostgreSQL
- OpenAI GPT API

### Frontend (knockFE)
- React 19
- TypeScript
- Vite
- React Router
- TanStack Query (React Query)
- Recharts

## 프로젝트 구조

```
stockknock/
├── knockBE/          # 백엔드 (Spring Boot)
│   └── src/main/java/com/sxxm/stockknock/
│       ├── entity/       # JPA 엔티티
│       ├── repository/   # 데이터베이스 접근
│       ├── service/      # 비즈니스 로직
│       ├── controller/   # REST API
│       ├── dto/          # 데이터 전송 객체
│       ├── config/       # 설정
│       ├── security/     # 보안 설정
│       └── ai/           # AI 서비스
│
└── knockFE/          # 프론트엔드 (React)
    └── src/
        ├── api/          # API 클라이언트
        ├── pages/        # 페이지 컴포넌트
        ├── components/   # 재사용 컴포넌트
        ├── context/      # React Context
        └── utils/        # 유틸리티
```

## 설치 및 실행 방법

### 사전 요구사항

- Java 17 이상
- Node.js 18 이상
- PostgreSQL 12 이상
- OpenAI API Key (선택사항 - AI 기능 사용 시)

### 데이터베이스 설정

자세한 PostgreSQL 설정은 [POSTGRESQL_SETUP.md](./POSTGRESQL_SETUP.md)를 참고하세요.

1. PostgreSQL에 접속:
```bash
psql postgres
```

2. 데이터베이스 및 사용자 생성:
```sql
CREATE DATABASE stockknockDB;
CREATE USER sxxm WITH PASSWORD 'sxxmpass';
GRANT ALL PRIVILEGES ON DATABASE stockknockDB TO sxxm;
ALTER DATABASE stockknockDB OWNER TO sxxm;

-- PostgreSQL 15+ 사용 시 추가 권한
\c stockknockDB
GRANT ALL ON SCHEMA public TO sxxm;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO sxxm;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO sxxm;
```

### Backend 설정 및 실행

1. knockBE 디렉토리로 이동:
```bash
cd knockBE
```

2. application.properties 설정 확인:
   - 데이터베이스 연결 정보 확인
   - OpenAI API Key 설정 (선택사항):
     - `openai.api.key=your-api-key-here`
     - 또는 환경 변수 `OPENAI_API_KEY` 설정

3. Gradle로 프로젝트 빌드:
```bash
./gradlew build
```

4. 애플리케이션 실행:
```bash
./gradlew bootRun
```

백엔드는 `http://localhost:8080`에서 실행됩니다.

### Frontend 설정 및 실행

1. knockFE 디렉토리로 이동:
```bash
cd knockFE
```

2. 의존성 설치:
```bash
npm install
```

3. 개발 서버 실행:
```bash
npm run dev
```

프론트엔드는 `http://localhost:3000`에서 실행됩니다.

## 환경 변수 설정

### Backend (.env 또는 application.properties)

```properties
# OpenAI API Key (선택사항)
OPENAI_API_KEY=your-api-key-here

# JWT Secret (프로덕션 환경에서는 반드시 변경)
JWT_SECRET=stockknock-secret-key-change-in-production
```

## API 문서

자세한 API 목록은 [API_LIST.md](./API_LIST.md)를 참고하세요.

## 주요 API 엔드포인트

- `POST /api/auth/register` - 회원가입
- `POST /api/auth/login` - 로그인
- `GET /api/stocks/symbol/{symbol}` - 종목 조회
- `GET /api/portfolio` - 포트폴리오 조회
- `GET /api/news/recent` - 최근 뉴스 조회
- `POST /api/ai/chat` - AI 채팅

## 개발 참고사항

### 데이터베이스 스키마

JPA의 `spring.jpa.hibernate.ddl-auto=update` 설정으로 자동으로 테이블이 생성됩니다.

### 인증

JWT 토큰 기반 인증을 사용합니다. 로그인/회원가입 시 발급된 토큰을 헤더에 포함해야 합니다:

```
Authorization: Bearer {token}
```

### CORS

프로덕션 환경에서는 CORS 설정을 적절히 변경해야 합니다.

## 문제 해결

### 데이터베이스 연결 오류
- PostgreSQL 서버가 실행 중인지 확인
- 데이터베이스 사용자 권한 확인
- `application.properties`의 연결 정보 확인
- PostgreSQL 인증 설정(`pg_hba.conf`) 확인

### OpenAI API 오류
- API Key가 올바른지 확인
- API Key에 충분한 크레딧이 있는지 확인
- API Key가 없어도 다른 기능은 정상 작동합니다

## 향후 개선 사항

- [ ] 실시간 주가 업데이트 (WebSocket)
- [ ] 포트폴리오 주간 리포트 자동 생성
- [ ] 실적 발표 캘린더 기능
- [ ] 산업군 분석 기능
- [ ] 가격 알림 실시간 체크 스케줄러
- [ ] 주가 예측 기능 고도화
- [ ] 차트 시각화 기능

## 라이선스

이 프로젝트는 개인 학습 목적으로 제작되었습니다.

