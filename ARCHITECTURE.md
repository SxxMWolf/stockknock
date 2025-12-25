# Spring + FastAPI 하이브리드 구조 제안서

## 📊 현재 상황 분석

### 현재 구조의 한계
- **AI 처리**: Java에서 OpenAI API 직접 호출 → Python 라이브러리 활용 불가
- **비동기 처리**: Spring의 WebClient 사용 → FastAPI의 async/await 대비 성능 제한
- **AI 생태계**: Python의 풍부한 AI 라이브러리 (pandas, numpy, scikit-learn 등) 미활용
- **개발 속도**: AI 기능 추가 시 Java 코드 작성 → Python 대비 개발 시간 증가

### 하이브리드 구조의 장점

#### ✅ 1. 기술적 장점
- **FastAPI (Python)**
  - AI 라이브러리 직접 활용 (OpenAI SDK, LangChain, HuggingFace 등)
  - 비동기 처리 성능 우수 (async/await)
  - 데이터 분석 라이브러리 활용 (pandas, numpy)
  - 개발 속도 향상 (AI 기능 구현)
  
- **Spring Boot (Java)**
  - 안정적인 인증/보안 (Spring Security)
  - 엔터프라이즈급 트랜잭션 관리
  - 검증된 ORM (JPA/Hibernate)
  - 강력한 타입 안정성

#### ✅ 2. 비용 최적화
- **AI API 호출 최적화**: Python의 배치 처리로 API 호출 횟수 감소
- **비동기 처리**: FastAPI의 async로 동시 처리량 증가 → 서버 리소스 효율
- **캐싱 전략**: Python의 메모리 효율적인 캐싱

#### ✅ 3. 확장성
- **독립적 스케일링**: AI 서비스만 별도로 스케일 아웃 가능
- **기술 스택 분리**: 각 서비스에 최적의 기술 선택
- **마이크로서비스 전환 용이**: 향후 완전한 마이크로서비스로 전환 가능

---

## 🏗 제안하는 아키텍처

```
┌─────────────────────────────────────────────────────────────┐
│                        Frontend (React)                       │
│                    http://localhost:5173                     │
│                                                               │
│  ✅ 모든 API 요청은 Spring Boot로만 전송                      │
│  ✅ FastAPI는 직접 호출하지 않음                              │
└───────────────────────────┬───────────────────────────────────┘
                            │
                            │ HTTP/REST (Spring Boot만)
                            │
                            ▼
              ┌───────────────────────────────────┐
              │      Spring Boot (Port 8080)      │
              │                                    │
              │  ✅ 인증/사용자 관리                │
              │  ✅ 포트폴리오 관리                 │
              │  ✅ 관심 종목 관리                  │
              │  ✅ 가격 알림 관리                  │
              │  ✅ 마이페이지 (개인정보 관리)      │
              │  ✅ Swagger UI                      │
              │                                    │
              │  ┌─────────────────────────────┐  │
              │  │  내부 FastAPI 호출           │  │
              │  │  (WebClient 사용)            │  │
              │  └───────────┬─────────────────┘  │
              └──────────────┼────────────────────┘
                             │
                             │ HTTP (내부 통신)
                             │
                             ▼
              ┌───────────────────────────────────┐
              │      FastAPI (Port 8000)          │
              │                                    │
              │  ✅ AI 서비스 (GPT-4)              │
              │  ✅ 뉴스 수집/분석                 │
              │  ✅ 주가 데이터 수집               │
              │  ✅ 스케줄러                       │
              └──────────────┬────────────────────┘
                             │
                             │
                             ▼
              ┌──────────────────────┐
              │   PostgreSQL         │
              │   (공유 데이터베이스)  │
              └──────────────────────┘
```

### 서비스 분리 계획

#### Spring Boot (knockBE) - 안정성 중심, 프론트엔드 진입점
```
✅ 인증 및 사용자 관리
  - 회원가입/로그인 (아이디 기반)
  - JWT 토큰 발급/검증
  - 마이페이지 - 개인정보 관리
    - 아이디 변경
    - 닉네임 변경
    - 이메일 변경 (인증 코드 기반)
    - 비밀번호 변경
  - 사용자 프로필 조회/수정

✅ 포트폴리오 관리
  - 포트폴리오 CRUD
  - 실시간 손익 계산 (FastAPI에서 가격 조회)
  - AI 포트폴리오 분석 (FastAPI 호출)

✅ 관심 종목 관리
  - 관심 종목 추가/삭제/조회

✅ 가격 알림 관리
  - 알림 설정 CRUD
  - 알림 트리거 체크 (FastAPI에서 가격 조회)

✅ 뉴스 관리
  - 뉴스 조회 (FastAPI에서 수집된 뉴스)
  - 뉴스 AI 분석 (FastAPI 호출)

✅ AI 채팅
  - AI 채팅 요청 처리 (FastAPI 호출)

✅ API 문서
  - Swagger UI 제공
  - OpenAPI 3.0 명세
```

#### FastAPI (knockAI) - AI 및 데이터 처리 중심, 내부 서비스
```
✅ AI 서비스 (Spring Boot에서 호출)
  - AI 채팅 (GPT-4)
  - 포트폴리오 AI 분석
  - 뉴스 AI 분석

✅ 뉴스 처리
  - 뉴스 수집 (NewsAPI)
  - 뉴스 분석 (GPT-4)
  - 뉴스-종목 연관 분석
  - 중복 뉴스 제거

✅ 주가 처리
  - 주가 데이터 수집 (Yahoo Finance, Alpha Vantage, Twelve Data)
  - 주가 히스토리 저장
  - 실시간 가격 조회 API (Spring Boot에서 호출)

✅ 스케줄러
  - 주가 업데이트 스케줄러
  - 뉴스 수집 스케줄러
  - 알림 체크 스케줄러 (Spring Boot에 알림 전송)

⚠️ 중요: FastAPI는 프론트엔드에서 직접 호출하지 않음
  - 모든 요청은 Spring Boot를 통해 처리
  - Spring Boot가 내부적으로 FastAPI를 호출
  - 보안, 로깅, 제한 등을 Spring Boot에서 중앙 관리
```

---

## 🔄 서비스 간 통신 설계

### 1. Spring → FastAPI 호출

**사용 사례:**
- 포트폴리오 조회 시 최신 가격 필요
- 가격 알림 체크 시 현재 가격 필요
- AI 분석 요청

**구현 방법:**
```java
// Spring Boot에서 FastAPI 호출
@Service
public class StockPriceService {
    private final WebClient fastApiClient;
    
    public BigDecimal getCurrentPrice(String symbol) {
        return fastApiClient
            .get()
            .uri("http://localhost:8000/api/stock/{symbol}/price", symbol)
            .retrieve()
            .bodyToMono(BigDecimal.class)
            .block();
    }
}
```

### 2. 프론트엔드 → Spring Boot 호출 (단일 진입점)

**중요 원칙:**
- **프론트엔드는 오직 Spring Boot API만 호출**
- FastAPI는 프론트엔드에서 직접 호출하지 않음
- 모든 요청은 Spring Boot를 통해 처리

**API 구조:**
```
프론트엔드 → Spring Boot (/api/*)
           ↓
      FastAPI (내부 호출, /api/*)
```

**장점:**
- 보안 중앙 관리 (JWT, CORS, Rate Limiting)
- 로깅 및 모니터링 통합
- API 키 관리 중앙화
- 에러 처리 일관성
- 향후 마이크로서비스 전환 용이

### 3. FastAPI → Spring 호출 (선택적)

**사용 사례:**
- 알림 트리거 시 Spring에 알림 전송 요청
- 사용자 정보 조회 (선택적)

**구현 방법:**
```python
# FastAPI에서 Spring 호출
import httpx

async def send_alert_to_spring(user_id: int, alert_data: dict):
    async with httpx.AsyncClient() as client:
        response = await client.post(
            f"http://localhost:8080/api/alerts/trigger",
            json=alert_data,
            headers={"Authorization": f"Bearer {internal_token}"}
        )
        return response.json()
```

### 3. 공유 데이터베이스

**PostgreSQL 공유:**
- Spring: JPA/Hibernate 사용
- FastAPI: SQLAlchemy 사용
- 동일한 스키마 사용

**주의사항:**
- 트랜잭션 경계 명확히 정의
- 동시성 제어 (Optimistic Locking)
- 데이터 일관성 보장

---

## 📁 새로운 프로젝트 구조

```
stockknock/
├── knockBE/              # Spring Boot (기존)
│   ├── src/main/java/
│   │   └── com/sxxm/stockknock/
│   │       ├── auth/          # 인증 (유지)
│   │       ├── portfolio/     # 포트폴리오 (유지)
│   │       ├── watchlist/     # 관심 종목 (유지)
│   │       ├── alert/         # 알림 관리 (유지)
│   │       └── common/        # 공통 (FastAPI 클라이언트 추가)
│   └── build.gradle
│
├── knockAI/              # FastAPI (신규)
│   ├── app/
│   │   ├── main.py           # FastAPI 앱 진입점
│   │   ├── api/
│   │   │   ├── ai.py          # AI 채팅, 분석
│   │   │   ├── news.py        # 뉴스 수집/분석
│   │   │   └── stock.py       # 주가 조회/업데이트
│   │   ├── services/
│   │   │   ├── ai_service.py
│   │   │   ├── news_service.py
│   │   │   └── stock_service.py
│   │   ├── models/            # SQLAlchemy 모델
│   │   ├── schemas/           # Pydantic 스키마
│   │   ├── schedulers/        # APScheduler
│   │   └── database.py        # DB 연결
│   ├── requirements.txt
│   └── Dockerfile
│
├── knockFE/              # React (기존, 수정 완료)
│   └── src/
│       └── api/
│           ├── client.ts      # Spring Boot API 클라이언트 (기본)
│           ├── fastApiClient.ts # FastAPI 클라이언트 (내부 사용, 현재 미사용)
│           ├── auth.ts        # Spring Boot 호출
│           ├── portfolio.ts   # Spring Boot 호출
│           ├── ai.ts          # Spring Boot 호출 (내부에서 FastAPI 호출)
│           ├── news.ts        # Spring Boot 호출 (내부에서 FastAPI 호출)
│           ├── stock.ts       # Spring Boot 호출
│           ├── watchlist.ts   # Spring Boot 호출
│           └── alerts.ts      # Spring Boot 호출
│
└── docker-compose.yml    # 통합 실행 (선택사항)
```

---

## 🚀 마이그레이션 현황

### ✅ 완료된 작업

#### Phase 1: FastAPI 서비스 구축 ✅
1. **FastAPI 프로젝트 생성** ✅
   - 기본 구조 설정 완료
   - PostgreSQL 연결 (SQLAlchemy) 완료
   - 공통 모델 정의 완료

2. **주가 서비스** ✅
   - 주가 수집 로직 → FastAPI
   - 주가 조회 API 제공
   - Spring에서 FastAPI 호출하도록 변경

#### Phase 2: AI 서비스 이전 ✅
1. **AI 서비스** ✅
   - AI 채팅 → FastAPI (Spring Boot가 내부 호출)
   - 뉴스 분석 → FastAPI (Spring Boot가 내부 호출)
   - 포트폴리오 분석 → FastAPI (Spring Boot가 내부 호출)

2. **뉴스 서비스** ✅
   - 뉴스 수집 → FastAPI
   - 뉴스 분석 → FastAPI
   - Spring에서 FastAPI 호출하도록 변경
   - **프론트엔드는 Spring Boot API만 호출** ✅

#### Phase 3: 프론트엔드 통합 ✅
1. **API 클라이언트 통일** ✅
   - 모든 프론트엔드 API가 Spring Boot만 호출
   - FastAPI는 백엔드 내부 서비스로만 사용
   - watchlist, alerts API 추가

2. **마이페이지 기능** ✅
   - 아이디 변경
   - 닉네임 변경
   - 이메일 변경
   - 비밀번호 변경

#### Phase 4: API 문서화 ✅
1. **Swagger UI 추가** ✅
   - SpringDoc OpenAPI 통합
   - API 문서 자동 생성
   - JWT 인증 지원

### 🔄 현재 아키텍처

**프론트엔드 → Spring Boot → FastAPI → PostgreSQL**

- 프론트엔드는 오직 Spring Boot API만 호출
- Spring Boot가 필요 시 FastAPI를 내부적으로 호출
- 모든 데이터는 PostgreSQL에 저장
- 보안, 로깅, 제한은 Spring Boot에서 중앙 관리

2. **통합 테스트**
   - 서비스 간 통신 테스트
   - 데이터 일관성 테스트
   - 부하 테스트

---

## 💰 비용 및 성능 개선 예상

### 비용 절감
- **AI API 호출**: Python 배치 처리로 20-30% 감소 예상
- **서버 리소스**: 비동기 처리로 동일 리소스에서 더 많은 요청 처리

### 성능 개선
- **AI 응답 시간**: Python 라이브러리 활용으로 10-20% 개선
- **동시 처리량**: FastAPI async로 2-3배 증가 예상
- **주가 업데이트**: 비동기 처리로 지연 시간 감소

---

## ⚠️ 고려사항 및 리스크

### 단점
1. **복잡도 증가**
   - 두 개의 서버 관리
   - 서비스 간 통신 오류 처리
   - 배포 복잡도 증가

2. **개발 환경**
   - Java + Python 환경 설정
   - 두 언어의 의존성 관리

3. **디버깅**
   - 분산 시스템 디버깅 어려움
   - 로그 추적 복잡도 증가

### 완화 방안
1. **API Gateway 도입** (선택사항)
   - Kong, Traefik 등으로 통합 엔드포인트 제공
   - 로깅 및 모니터링 통합

2. **로깅 및 모니터링**
   - ELK Stack 또는 Grafana로 통합 모니터링
   - 분산 추적 (Jaeger, Zipkin)

3. **Docker Compose**
   - 개발 환경 통합 실행
   - 배포 자동화

---

## 🎯 결론 및 권장사항

### ✅ 하이브리드 구조를 추천하는 경우
- AI 기능이 계속 확장될 예정
- 비용 최적화가 중요
- 빠른 AI 기능 개발이 필요
- Python AI 라이브러리를 활용하고 싶음

### ❌ 단일 구조를 유지하는 경우
- 현재 구조로 충분히 만족
- 운영 복잡도를 최소화하고 싶음
- 팀이 Java만 다룰 수 있음
- 빠른 배포가 우선

### 💡 중간 방안: 점진적 전환
1. **1단계**: 주가 서비스만 FastAPI로 이전 (리스크 최소)
2. **2단계**: AI 서비스 이전 (비용 절감 효과 확인)
3. **3단계**: 뉴스 서비스 이전 (완전한 하이브리드)

---

## 📝 다음 단계

1. **프로토타입 개발** (1주)
   - FastAPI 기본 구조 구축
   - 주가 조회 API 1개 구현

---

## 🔌 외부 API 연동 상세

### 📊 주가 API (Stock Price APIs)

StockKnock은 여러 주가 API를 지원하며, 우선순위에 따라 자동으로 폴백(fallback)합니다.

#### 1. Yahoo Finance (기본, 무료) ⭐

**우선순위: 1순위 (기본 사용)**

- **URL**: `https://query1.finance.yahoo.com/v8/finance/chart/{symbol}`
- **API 키**: 불필요 (무료)
- **제한**: 초당 2회 요청
- **설정**: `application.properties` → `stock.api.yahoo-finance.enabled=true` (기본값)
- **용도**: 주식 가격, 일일 고가/저가, 거래량 등 실시간 정보
- **장점**: 무료, API 키 불필요, 안정적
- **단점**: 비공식 API (공식 지원 없음)

#### 2. Alpha Vantage (백업 옵션)

**우선순위: 2순위 (Yahoo Finance 실패 시)**

- **URL**: `https://www.alphavantage.co/query`
- **API 키**: 필수 (환경 변수 `ALPHA_VANTAGE_API_KEY`)
- **제한**: 무료 플랜 일일 25회, 분당 5회 요청
- **설정**: `application.properties` → `stock.api.alpha-vantage.key=${ALPHA_VANTAGE_API_KEY:}`
- **용도**: 주식 가격, 전일 종가, 고가/저가, 거래량
- **장점**: 공식 API, 안정적
- **단점**: 무료 플랜 제한이 엄격함

#### 3. Twelve Data (백업 옵션)

**우선순위: 3순위 (Yahoo Finance, Alpha Vantage 실패 시)**

- **URL**: `https://api.twelvedata.com/price`
- **API 키**: 필수 (환경 변수 `TWELVE_DATA_API_KEY`)
- **제한**: 무료 플랜 일일 800회 요청
- **설정**: `application.properties` → `stock.api.twelve-data.key=${TWELVE_DATA_API_KEY:}`
- **용도**: 주식 가격 조회
- **장점**: 무료 플랜 제한이 넉넉함
- **단점**: 기본 가격 정보만 제공 (고가/저가 등 제한적)

**주가 API 우선순위:**
```
Yahoo Finance (기본) 
  → Alpha Vantage (실패 시)
    → Twelve Data (실패 시)
      → FastAPI (내부 서비스)
        → DB 저장된 최신 가격 (최종 폴백)
```

### 📰 뉴스 API (News API)

#### NewsAPI (newsapi.org)

**뉴스 수집 전용 API**

- **URL**: `https://newsapi.org/v2/everything`
- **API 키**: 필수 (환경 변수 `NEWS_API_KEY`)
- **제한**: 무료 플랜 일일 100회 요청
- **설정**: `application.properties` → `news.api.newsapi.key=${NEWS_API_KEY:}`
- **용도**: 주식 관련 뉴스 수집
- **언어**: 한국어 (`language=ko`)
- **정렬**: 발행일 기준 내림차순 (`sortBy=publishedAt`)

**뉴스 수집 전략:**
- **정기 자동 수집**: 하루 2회 (오전 9시, 오후 4시)
- **수동 수집**: 개발/관리자용 API (`POST /api/news/collect`)
- **테스트 데이터**: 개발용 API (`POST /api/news/test-data`)
- **중복 제거**: 제목 유사도 기반 자동 제거
- **AI 분석**: 수집된 뉴스 중 주요 20개만 분석 (비용 최적화)

### 🤖 AI API

#### OpenAI GPT API

**AI 분석 전용 API**

- **URL**: `https://api.openai.com/v1/chat/completions`
- **API 키**: 필수 (환경 변수 `OPENAI_API_KEY`)
- **모델**: GPT-4o-mini (기본값)
- **설정**: 
  - `application.properties` → `gpt.api.key=${OPENAI_API_KEY}`
  - `application.properties` → `gpt.model=${GPT_MODEL:gpt-4o-mini}`

**용도:**
- 뉴스 요약 및 주가 영향 분석
- 포트폴리오 AI 분석
- AI 채팅 (대화형 애널리스트)

**비용 최적화:**
- 주요 20개 뉴스만 AI 분석
- 골든 뉴스(주가 영향 높은 뉴스) 우선 분석
- 중복 뉴스 자동 제거
   - Spring에서 호출 테스트

2. **성능 비교 테스트**
   - 현재 구조 vs 하이브리드 구조
   - 응답 시간, 처리량 비교

3. **의사결정**
   - 테스트 결과 기반으로 최종 결정

---

**작성일**: 2024-01-XX  
**작성자**: AI Assistant  
**버전**: 1.0

