# StockKnock - AI 기반 통합 주식 분석 플랫폼

국내·해외 주식 투자자를 위한 AI 기반 주식 분석 플랫폼입니다.

---

## 📋 목차

1. [프로젝트 개요](#1-프로젝트-개요)
2. [기술 스택](#2-기술-스택)
3. [데이터베이스 설계](#3-데이터베이스-설계)
4. [구현된 기능](#4-구현된-기능)
5. [API 엔드포인트](#5-api-엔드포인트)
6. [프로젝트 구조](#6-프로젝트-구조)
7. [설치 및 실행](#7-설치-및-실행)
8. [환경 변수 설정](#8-환경-변수-설정)
9. [외부 API 연동](#9-외부-api-연동)
10. [개발 가이드](#10-개발-가이드)

---

## 1. 프로젝트 개요

StockKnock은 AI 기반 주식 분석 플랫폼으로, 다음과 같은 기능을 제공합니다:

- 📈 **실시간 주가 정보**: 국내·해외 주요 증시 주식 정보 조회 및 자동 업데이트
- 🤖 **AI 뉴스 분석**: GPT를 활용한 뉴스 자동 요약 및 주가 영향 분석
- 📊 **포트폴리오 관리**: 보유 종목 관리 및 손익 실시간 추적, AI 기반 종합 분석
- 🔔 **가격 알림**: 목표가/손절가/변동률 도달 시 자동 알림
- 💬 **AI 채팅**: 문맥을 유지하는 개인 애널리스트처럼 종목 전망, 산업 동향 분석
- 📰 **뉴스 피드**: 주요 증시 뉴스 자동 수집 및 AI 기반 종목 연관 분석
- ⭐ **관심 종목**: 관심 종목 관리

---

## 2. 기술 스택

### Backend (knockBE)
- **언어**: Java 17
- **프레임워크**: Spring Boot 4.0.0
- **데이터베이스**: PostgreSQL 12+
- **ORM**: Spring Data JPA (Hibernate)
- **보안**: Spring Security + JWT
- **AI**: OpenAI GPT-4 API
- **스케줄러**: Spring Scheduling
- **HTTP 클라이언트**: Spring WebFlux (WebClient)
- **이메일**: Spring Mail (선택사항)

### Frontend (knockFE)
- **언어**: TypeScript
- **프레임워크**: React 19
- **빌드 도구**: Vite
- **라우팅**: React Router
- **상태 관리**: TanStack Query (React Query)
- **HTTP 클라이언트**: Axios
- **차트**: Recharts

---

## 3. 데이터베이스 설계

### 3.1 ERD (Entity Relationship Diagram)

```
┌─────────────┐         ┌──────────────┐         ┌─────────────┐
│    User     │────────<│  Portfolio   │>────────│   Stock     │
│             │   1:N   │              │   N:1    │             │
└─────────────┘         └──────────────┘         └─────────────┘
      │                        │                        │
      │ 1:N                    │                        │ N:1
      │                        │                        │
┌─────────────┐         ┌──────────────┐         ┌─────────────┐
│ Watchlist  │         │ PriceAlert   │         │  Industry   │
└─────────────┘         └──────────────┘         └─────────────┘
      │                        │
      │                        │
      │                        │
┌─────────────┐         ┌──────────────┐
│    News     │<───────>│   Stock      │ (N:M)
│             │  N:M    │              │
└─────────────┘         └──────────────┘
      │
      │ 1:N
      │
┌─────────────┐
│NewsAnalysis │
└─────────────┘

┌─────────────┐
│AIConversation│
└─────────────┘
      │
      │ N:1
      │
┌─────────────┐
│    User     │
└─────────────┘
```

### 3.2 테이블 구조

#### users (사용자)
| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 사용자 ID |
| email | VARCHAR | UNIQUE, NOT NULL | 이메일 |
| password | VARCHAR | NOT NULL | 비밀번호 (BCrypt 해시) |
| name | VARCHAR | | 이름 |
| investment_style | VARCHAR | | 투자 스타일 (AGGRESSIVE, CONSERVATIVE, BALANCED) |
| created_at | TIMESTAMP | | 생성일시 |
| updated_at | TIMESTAMP | | 수정일시 |

#### stocks (주식)
| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 주식 ID |
| symbol | VARCHAR | UNIQUE, NOT NULL | 종목 코드 (AAPL, 005930) |
| name | VARCHAR | NOT NULL | 종목명 |
| exchange | VARCHAR | | 거래소 (NYSE, NASDAQ, KOSPI, KOSDAQ) |
| country | VARCHAR | | 국가 (US, KR) |
| industry_id | BIGINT | FK | 산업 ID |
| current_price | DECIMAL(20,2) | | 현재가 |
| previous_close | DECIMAL(20,2) | | 전일 종가 |
| day_high | DECIMAL(20,2) | | 당일 고가 |
| day_low | DECIMAL(20,2) | | 당일 저가 |
| volume | BIGINT | | 거래량 |
| market_cap | DECIMAL(20,2) | | 시가총액 |
| pe_ratio | DECIMAL(10,2) | | PER |
| dividend_yield | DECIMAL(5,2) | | 배당률 |
| last_updated | TIMESTAMP | | 마지막 업데이트 시간 |

#### portfolios (포트폴리오)
| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 포트폴리오 ID |
| user_id | BIGINT | FK, NOT NULL | 사용자 ID |
| stock_id | BIGINT | FK, NOT NULL | 주식 ID |
| quantity | DECIMAL(20,4) | | 보유 수량 |
| average_price | DECIMAL(20,2) | | 평균 매수가 |
| current_price | DECIMAL(20,2) | | 현재가 |
| total_value | DECIMAL(20,2) | | 총 평가액 |
| profit_loss | DECIMAL(20,2) | | 손익 |
| profit_loss_rate | DECIMAL(10,4) | | 손익률 (%) |
| purchased_at | TIMESTAMP | | 매수일시 |
| updated_at | TIMESTAMP | | 수정일시 |

#### news (뉴스)
| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 뉴스 ID |
| title | VARCHAR | | 제목 |
| content | TEXT | | 내용 |
| source | VARCHAR | | 출처 |
| url | VARCHAR | | 원문 URL |
| published_at | TIMESTAMP | | 발행일시 |
| created_at | TIMESTAMP | | 생성일시 |

#### news_stocks (뉴스-종목 연관)
| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| news_id | BIGINT | FK, PK | 뉴스 ID |
| stock_id | BIGINT | FK, PK | 주식 ID |

#### news_analyses (뉴스 분석)
| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 분석 ID |
| news_id | BIGINT | FK, NOT NULL | 뉴스 ID |
| summary | TEXT | | AI 요약 |
| impact_analysis | TEXT | | 주가 영향 분석 |
| sentiment | VARCHAR | | 감정 (POSITIVE, NEGATIVE, NEUTRAL) |
| impact_score | INTEGER | | 영향 점수 (1-10) |
| analyzed_at | TIMESTAMP | | 분석일시 |

#### price_alerts (가격 알림)
| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 알림 ID |
| user_id | BIGINT | FK, NOT NULL | 사용자 ID |
| stock_id | BIGINT | FK, NOT NULL | 주식 ID |
| alert_type | VARCHAR | | 알림 타입 (TARGET_PRICE, STOP_LOSS, PERCENTAGE_CHANGE) |
| target_price | DECIMAL(20,2) | | 목표가/손절가 |
| percentage_change | DECIMAL(5,2) | | 변동률 (%) |
| is_triggered | BOOLEAN | | 트리거 여부 |
| triggered_at | TIMESTAMP | | 트리거 일시 |
| created_at | TIMESTAMP | | 생성일시 |

#### watchlists (관심 종목)
| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 관심 종목 ID |
| user_id | BIGINT | FK, NOT NULL | 사용자 ID |
| stock_id | BIGINT | FK, NOT NULL | 주식 ID |
| added_at | TIMESTAMP | | 추가일시 |

#### ai_conversations (AI 대화)
| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 대화 ID |
| user_id | BIGINT | FK, NOT NULL | 사용자 ID |
| user_question | TEXT | | 사용자 질문 |
| ai_response | TEXT | | AI 응답 |
| conversation_type | VARCHAR | | 대화 타입 |
| created_at | TIMESTAMP | | 생성일시 |

#### industries (산업)
| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 산업 ID |
| name | VARCHAR | UNIQUE, NOT NULL | 산업명 |
| description | VARCHAR | | 설명 |

### 3.3 관계 요약

- **User ↔ Portfolio**: 1:N (한 사용자는 여러 포트폴리오 보유)
- **User ↔ Watchlist**: 1:N (한 사용자는 여러 관심 종목 보유)
- **User ↔ PriceAlert**: 1:N (한 사용자는 여러 알림 설정)
- **User ↔ AIConversation**: 1:N (한 사용자는 여러 AI 대화)
- **Stock ↔ Portfolio**: 1:N (한 종목은 여러 사용자의 포트폴리오에 포함)
- **Stock ↔ Watchlist**: 1:N (한 종목은 여러 사용자의 관심 종목에 포함)
- **Stock ↔ Industry**: N:1 (한 종목은 하나의 산업에 속함)
- **News ↔ Stock**: N:M (뉴스와 종목은 다대다 관계)
- **News ↔ NewsAnalysis**: 1:N (한 뉴스는 여러 분석 결과를 가질 수 있음)

---

## 4. 구현된 기능

### 4.1 인증 시스템 ✅
- JWT 기반 인증
- 회원가입/로그인
- 비밀번호 BCrypt 암호화
- 토큰 자동 관리 (localStorage)
- 보호된 라우트

### 4.2 주식 정보 조회 ✅
- 종목 코드로 조회
- 종목 검색 (이름 기반)
- 국가별 조회
- 산업별 조회

### 4.3 실시간 가격 업데이트 ✅
- **스케줄러**: 1분마다 자동 업데이트
- **지원 API**: Yahoo Finance (무료), Alpha Vantage, Twelve Data
- **우선순위**: Yahoo Finance → Alpha Vantage → Twelve Data
- **에러 처리**: API 실패 시 다음 API 시도

### 4.4 포트폴리오 관리 ✅
- 포트폴리오 조회
- 종목 추가/수정/삭제
- 손익 자동 계산 (총 평가액, 손익, 손익률)
- **AI 포트폴리오 분석**: 건강도 평가, 리스크 분석, 리밸런싱 제안
- Validation: 음수 체크, 0원 가격 체크

### 4.5 가격 알림 ✅
- **알림 타입**:
  - 목표가 도달 (TARGET_PRICE)
  - 손절가 도달 (STOP_LOSS)
  - 변동률 기준 (PERCENTAGE_CHANGE)
- **스케줄러**: 10초마다 알림 체크
- **트리거 시**: 알림 발송 및 자동 비활성화
- **알림 발송**: 이메일 (설정 시)

### 4.6 뉴스 수집 및 분석 ✅
- **자동 수집**: NewsAPI 연동
  - 매 시간마다 일반 주식 뉴스 수집
  - 매일 오전 9시 주요 종목 뉴스 수집
- **AI 분석**: GPT-4 기반
  - 뉴스 요약
  - 주가 영향 분석
  - 감정 분석 (POSITIVE, NEGATIVE, NEUTRAL)
  - 영향 점수 (1-10)
- **종목 연관**: AI를 활용한 뉴스-종목 자동 매핑

### 4.7 AI 채팅 ✅
- **문맥 유지**: 최근 5개 대화 기록 활용
- **대화 타입**: 종목 전망, 산업 동향, 리스크 분석 등
- **대화 기록 저장**: 사용자별 대화 이력 관리

### 4.8 관심 종목 ✅
- 관심 종목 추가/삭제
- 관심 종목 조회

---

## 5. API 엔드포인트

### 기본 정보
- **Base URL**: `http://localhost:8080/api`
- **인증**: JWT Token (Bearer Token)
- **Content-Type**: `application/json`

### 5.1 인증 (Authentication)

#### 회원가입
```
POST /api/auth/register
Body: { "email": "user@example.com", "password": "password123" }
Response: { "token": "...", "email": "...", "userId": 1 }
```

#### 로그인
```
POST /api/auth/login
Body: { "email": "user@example.com", "password": "password123" }
Response: { "token": "...", "email": "...", "userId": 1 }
```

### 5.2 주식 정보 (Stock)

#### 종목 조회
```
GET /api/stocks/symbol/{symbol}
Response: StockDto
```

#### 종목 검색
```
GET /api/stocks/search?keyword={keyword}
Response: StockDto[]
```

#### 국가별 조회
```
GET /api/stocks/country/{country}
Response: StockDto[]
```

#### 산업별 조회
```
GET /api/stocks/industry/{industryId}
Response: StockDto[]
```

### 5.3 포트폴리오 (Portfolio)

#### 포트폴리오 조회
```
GET /api/portfolio
Headers: Authorization: Bearer {token}
Response: PortfolioDto[]
```

#### 포트폴리오 추가
```
POST /api/portfolio?stockSymbol={symbol}&quantity={qty}&averagePrice={price}
Headers: Authorization: Bearer {token}
Response: PortfolioDto
```

#### 포트폴리오 수정
```
PUT /api/portfolio/{portfolioId}?quantity={qty}&averagePrice={price}
Headers: Authorization: Bearer {token}
Response: PortfolioDto
```

#### 포트폴리오 삭제
```
DELETE /api/portfolio/{portfolioId}
Headers: Authorization: Bearer {token}
```

#### AI 포트폴리오 분석
```
GET /api/portfolio/analysis
Headers: Authorization: Bearer {token}
Response: PortfolioAnalysisDto
```

### 5.4 관심 종목 (Watchlist)

#### 관심 종목 조회
```
GET /api/watchlist
Headers: Authorization: Bearer {token}
Response: StockDto[]
```

#### 관심 종목 추가
```
POST /api/watchlist/{stockSymbol}
Headers: Authorization: Bearer {token}
```

#### 관심 종목 제거
```
DELETE /api/watchlist/{stockSymbol}
Headers: Authorization: Bearer {token}
```

### 5.5 뉴스 (News)

#### 최근 뉴스 조회
```
GET /api/news/recent?days={days}
기본값: days=7
Response: NewsDto[]
```

#### 뉴스 상세 조회
```
GET /api/news/{newsId}
Response: NewsDto
```

#### 뉴스 AI 분석
```
POST /api/news/{newsId}/analyze
Response: NewsAnalysisDto
```

### 5.6 AI 채팅 (AI Chat)

#### AI와 대화
```
POST /api/ai/chat
Headers: Authorization: Bearer {token}
Body: { "question": "...", "conversationType": "..." }
Response: { "response": "...", "conversationType": "..." }
```

### 5.7 가격 알림 (Price Alert)

#### 알림 목록 조회
```
GET /api/alerts
Headers: Authorization: Bearer {token}
Response: PriceAlert[]
```

#### 알림 생성
```
POST /api/alerts?stockSymbol={symbol}&alertType={type}&targetPrice={price}
Headers: Authorization: Bearer {token}
Alert Types: TARGET_PRICE, STOP_LOSS, PERCENTAGE_CHANGE
Response: PriceAlert
```

#### 알림 삭제
```
DELETE /api/alerts/{alertId}
Headers: Authorization: Bearer {token}
```

---

## 6. 프로젝트 구조

```
stockknock/
├── knockBE/                          # 백엔드 (Spring Boot)
│   ├── build.gradle                  # Gradle 빌드 설정
│   ├── src/main/java/com/sxxm/stockknock/
│   │   ├── StockknockApplication.java    # 메인 애플리케이션
│   │   ├── entity/                       # JPA 엔티티
│   │   │   ├── User.java
│   │   │   ├── Stock.java
│   │   │   ├── Portfolio.java
│   │   │   ├── News.java
│   │   │   ├── NewsAnalysis.java
│   │   │   ├── PriceAlert.java
│   │   │   ├── Watchlist.java
│   │   │   ├── AIConversation.java
│   │   │   └── Industry.java
│   │   ├── repository/                  # 데이터베이스 접근
│   │   │   ├── UserRepository.java
│   │   │   ├── StockRepository.java
│   │   │   ├── PortfolioRepository.java
│   │   │   └── ...
│   │   ├── service/                     # 비즈니스 로직
│   │   │   ├── UserService.java
│   │   │   ├── StockService.java
│   │   │   ├── PortfolioService.java
│   │   │   ├── NewsService.java
│   │   │   ├── StockPriceService.java      # 주가 업데이트
│   │   │   ├── NewsCrawlerService.java      # 뉴스 수집
│   │   │   ├── NewsStockAssociationService.java  # 뉴스-종목 연관
│   │   │   └── NotificationService.java    # 알림 발송
│   │   ├── controller/                  # REST API
│   │   │   ├── AuthController.java
│   │   │   ├── StockController.java
│   │   │   ├── PortfolioController.java
│   │   │   ├── NewsController.java
│   │   │   ├── AIController.java
│   │   │   ├── PriceAlertController.java
│   │   │   └── WatchlistController.java
│   │   ├── dto/                         # 데이터 전송 객체
│   │   │   ├── StockDto.java
│   │   │   ├── PortfolioDto.java
│   │   │   ├── PortfolioAnalysisDto.java
│   │   │   ├── NewsDto.java
│   │   │   └── ...
│   │   ├── config/                      # 설정
│   │   │   ├── SecurityConfig.java
│   │   │   ├── WebConfig.java
│   │   │   └── ValidationConfig.java
│   │   ├── security/                    # 보안
│   │   │   └── JwtAuthenticationFilter.java
│   │   ├── scheduler/                   # 스케줄러
│   │   │   ├── StockPriceScheduler.java
│   │   │   ├── PriceAlertScheduler.java
│   │   │   └── NewsCrawlerScheduler.java
│   │   ├── ai/                          # AI 서비스
│   │   │   └── AIService.java
│   │   └── util/                        # 유틸리티
│   │       └── JwtUtil.java
│   └── src/main/resources/
│       └── application.properties       # 설정 파일
│
└── knockFE/                             # 프론트엔드 (React)
    ├── package.json
    ├── vite.config.ts
    └── src/
        ├── main.tsx
        ├── App.tsx
        ├── api/                          # API 클라이언트
        │   ├── client.ts
        │   ├── auth.ts
        │   ├── stock.ts
        │   ├── portfolio.ts
        │   ├── news.ts
        │   └── ai.ts
        ├── pages/                        # 페이지 컴포넌트
        │   ├── Login.tsx
        │   ├── Dashboard.tsx
        │   ├── Portfolio.tsx
        │   ├── News.tsx
        │   └── AIChat.tsx
        ├── context/                      # React Context
        │   └── AuthContext.tsx
        └── assets/
```

---

## 7. 설치 및 실행

### 7.1 사전 요구사항

- Java 17 이상
- Node.js 18 이상
- PostgreSQL 12 이상
- OpenAI API Key (선택사항 - AI 기능 사용 시)

### 7.2 데이터베이스 설정

1. PostgreSQL에 접속:
```bash
psql postgres
```

2. 데이터베이스 및 사용자 생성:
```sql
CREATE DATABASE stockknockdb;
CREATE USER sxxm WITH PASSWORD 'sxxmpass';
GRANT ALL PRIVILEGES ON DATABASE stockknockdb TO sxxm;
ALTER DATABASE stockknockdb OWNER TO sxxm;

-- PostgreSQL 15+ 사용 시 추가 권한
\c stockknockdb
GRANT ALL ON SCHEMA public TO sxxm;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO sxxm;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO sxxm;
```

3. `application.properties`에서 데이터베이스 연결 정보 확인:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/stockknockdb
spring.datasource.username=sxxm
spring.datasource.password=sxxmpass
```

### 7.3 Backend 설정 및 실행

1. knockBE 디렉토리로 이동:
```bash
cd knockBE
```

2. Gradle로 프로젝트 빌드:
```bash
./gradlew build
```

3. 애플리케이션 실행:
```bash
./gradlew bootRun
```

백엔드는 `http://localhost:8080`에서 실행됩니다.

### 7.4 Frontend 설정 및 실행

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

---

## 8. 환경 변수 설정

### 8.1 필수 설정

```properties
# application.properties 또는 환경 변수

# JWT
jwt.secret=${JWT_SECRET:stockknock-secret-key-change-in-production}
jwt.expiration=86400000  # 24시간

# 데이터베이스
spring.datasource.url=jdbc:postgresql://localhost:5432/stockknockdb
spring.datasource.username=sxxm
spring.datasource.password=sxxmpass
```

### 8.2 선택적 설정 (AI 기능)

```properties
# OpenAI API (AI 기능 사용 시 필수)
openai.api.key=${OPENAI_API_KEY:your-api-key-here}
```

### 8.3 선택적 설정 (주식 가격 API)

```properties
# Yahoo Finance (무료, 기본 활성화)
stock.api.yahoo-finance.enabled=true

# Alpha Vantage (선택사항)
stock.api.alpha-vantage.key=${ALPHA_VANTAGE_API_KEY:}

# Twelve Data (선택사항)
stock.api.twelve-data.key=${TWELVE_DATA_API_KEY:}
```

### 8.4 선택적 설정 (뉴스 API)

```properties
# NewsAPI (선택사항)
news.api.newsapi.key=${NEWS_API_KEY:}
news.api.enabled=true
```

### 8.5 선택적 설정 (이메일 알림)

```properties
# 이메일 알림 (선택사항)
spring.mail.host=${SPRING_MAIL_HOST:smtp.gmail.com}
spring.mail.port=${SPRING_MAIL_PORT:587}
spring.mail.username=${SPRING_MAIL_USERNAME:}
spring.mail.password=${SPRING_MAIL_PASSWORD:}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
notification.email.enabled=${NOTIFICATION_EMAIL_ENABLED:false}
```

### 8.6 환경 변수 예시 (.env)

```bash
# OpenAI
OPENAI_API_KEY=sk-your-api-key-here

# 주식 가격 API
ALPHA_VANTAGE_API_KEY=your-key
TWELVE_DATA_API_KEY=your-key

# 뉴스 API
NEWS_API_KEY=your-key

# 이메일
SPRING_MAIL_USERNAME=your-email@gmail.com
SPRING_MAIL_PASSWORD=your-app-password
NOTIFICATION_EMAIL_ENABLED=true

# JWT
JWT_SECRET=your-secret-key-here
```

---

## 9. 외부 API 연동

### 9.1 주식 가격 API

#### Yahoo Finance (무료, 추천)
- **특징**: 무료, API 키 불필요
- **제한**: 초당 2회 요청
- **설정**: `stock.api.yahoo-finance.enabled=true`

#### Alpha Vantage
- **무료 플랜**: 일일 25회, 분당 5회
- **가입**: https://www.alphavantage.co/support/#api-key
- **설정**: `stock.api.alpha-vantage.key=YOUR_KEY`

#### Twelve Data
- **무료 플랜**: 일일 800회
- **가입**: https://twelvedata.com/
- **설정**: `stock.api.twelve-data.key=YOUR_KEY`

### 9.2 뉴스 API

#### NewsAPI
- **무료 플랜**: 일일 100회
- **가입**: https://newsapi.org/register
- **설정**: `news.api.newsapi.key=YOUR_KEY`

### 9.3 AI 서비스

#### OpenAI GPT-4
- **비용**: 입력 $0.03/1K 토큰, 출력 $0.06/1K 토큰
- **가입**: https://platform.openai.com/
- **설정**: `openai.api.key=YOUR_KEY`
- **비용 절감**: GPT-3.5-turbo 사용 권장 (더 저렴)

### 9.4 이메일 알림

#### Gmail SMTP
- **설정**: Gmail 계정에서 "앱 비밀번호" 생성 필요
- **2단계 인증**: 필수
- **설정**: `spring.mail.*` 속성 설정

---

## 10. 개발 가이드

### 10.1 데이터베이스 스키마

JPA의 `spring.jpa.hibernate.ddl-auto=update` 설정으로 자동으로 테이블이 생성됩니다.

### 10.2 인증

JWT 토큰 기반 인증을 사용합니다. 로그인/회원가입 시 발급된 토큰을 헤더에 포함해야 합니다:

```
Authorization: Bearer {token}
```

### 10.3 스케줄러

다음 스케줄러가 자동 실행됩니다:

- **가격 알림 체크**: 10초마다
- **주식 가격 업데이트**: 1분마다
- **뉴스 수집**: 1시간마다
- **주요 종목 뉴스**: 매일 오전 9시

### 10.4 CORS

프로덕션 환경에서는 CORS 설정을 적절히 변경해야 합니다:
```java
configuration.setAllowedOrigins(List.of("https://yourdomain.com"));
```

### 10.5 Validation

입력 데이터 검증이 구현되어 있습니다:
- 포트폴리오: 보유량/평균가 음수 체크
- 가격 알림: 조건 검증
- 전역 예외 처리: `@RestControllerAdvice`

### 10.6 문제 해결

#### 데이터베이스 연결 오류
- PostgreSQL 서버가 실행 중인지 확인
- 데이터베이스 사용자 권한 확인
- `application.properties`의 연결 정보 확인

#### OpenAI API 오류
- API Key가 올바른지 확인
- API Key에 충분한 크레딧이 있는지 확인
- API Key가 없어도 다른 기능은 정상 작동합니다

#### API 제한 초과
- 요청 빈도 줄이기 (스케줄러 간격 조정)
- 캐싱 활용
- 유료 플랜으로 업그레이드

---

## 11. 향후 개선 사항

- [ ] 실시간 주가 업데이트 (WebSocket)
- [ ] 포트폴리오 주간 리포트 자동 생성 (PDF)
- [ ] 실적 발표 캘린더 기능
- [ ] 산업군 분석 기능
- [ ] AI 기반 종목 추천 엔진
- [ ] 시장 심리 지수 (Sentiment Index)
- [ ] 주가 예측 ML 모델
- [ ] TradingView 차트 연동
- [ ] 사용자 이벤트 기록 기반 추천

---

## 12. 라이선스

이 프로젝트는 개인 학습 목적으로 제작되었습니다.

---

## 13. 참고 자료

- [Spring Boot 공식 문서](https://spring.io/projects/spring-boot)
- [React 공식 문서](https://react.dev/)
- [OpenAI API 문서](https://platform.openai.com/docs)
- [PostgreSQL 공식 문서](https://www.postgresql.org/docs/)

---

**최종 업데이트**: 2024년

