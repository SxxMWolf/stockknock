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
│                    http://localhost:3000                     │
└───────────────────────────┬───────────────────────────────────┘
                            │
                            │ HTTP/REST
                            │
        ┌───────────────────┴───────────────────┐
        │                                         │
        ▼                                         ▼
┌───────────────────┐                  ┌───────────────────┐
│   Spring Boot     │                  │     FastAPI        │
│   (Port 8080)     │                  │   (Port 8000)      │
│                   │                  │                    │
│  ✅ 인증/사용자    │◄─────HTTP───────►│  ✅ AI 서비스       │
│  ✅ 포트폴리오     │                  │  ✅ 뉴스 분석       │
│  ✅ 관심 종목      │                  │  ✅ 주가 업데이트   │
│  ✅ 가격 알림      │                  │  ✅ 스케줄러        │
└─────────┬─────────┘                  └─────────┬─────────┘
          │                                        │
          │                                        │
          └──────────────┬────────────────────────┘
                         │
                         ▼
              ┌──────────────────────┐
              │   PostgreSQL         │
              │   (공유 데이터베이스)  │
              └──────────────────────┘
```

### 서비스 분리 계획

#### Spring Boot (knockBE) - 안정성 중심
```
✅ 인증 및 사용자 관리
  - 회원가입/로그인
  - JWT 토큰 발급/검증
  - 이메일 변경
  - 사용자 프로필 관리

✅ 포트폴리오 관리
  - 포트폴리오 CRUD
  - 실시간 손익 계산 (FastAPI에서 가격 조회)

✅ 관심 종목 관리
  - 관심 종목 추가/삭제

✅ 가격 알림 관리
  - 알림 설정 CRUD
  - 알림 트리거 체크 (FastAPI에서 가격 조회)
```

#### FastAPI (knockAI) - AI 및 데이터 처리 중심
```
✅ AI 서비스
  - AI 채팅 (GPT-4)
  - 포트폴리오 AI 분석
  - 뉴스 AI 분석

✅ 뉴스 처리
  - 뉴스 수집 (NewsAPI)
  - 뉴스 분석 (GPT-4)
  - 뉴스-종목 연관 분석
  - 중복 뉴스 제거

✅ 주가 처리
  - 주가 데이터 수집 (Yahoo Finance, Alpha Vantage)
  - 주가 히스토리 저장
  - 실시간 가격 조회 API

✅ 스케줄러
  - 주가 업데이트 스케줄러
  - 뉴스 수집 스케줄러
  - 알림 체크 스케줄러 (Spring에 알림 전송)
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

### 2. FastAPI → Spring 호출

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
├── knockFE/              # React (기존, 수정 필요)
│   └── src/
│       └── api/
│           ├── auth.ts        # Spring 호출
│           ├── portfolio.ts   # Spring 호출
│           ├── ai.ts          # FastAPI 호출
│           ├── news.ts        # FastAPI 호출
│           └── stock.ts       # FastAPI 호출
│
└── docker-compose.yml    # 통합 실행 (선택사항)
```

---

## 🚀 마이그레이션 계획

### Phase 1: FastAPI 서비스 구축 (1-2주)
1. **FastAPI 프로젝트 생성**
   - 기본 구조 설정
   - PostgreSQL 연결 (SQLAlchemy)
   - 공통 모델 정의

2. **주가 서비스 이전**
   - 주가 수집 로직 → FastAPI
   - 주가 조회 API 제공
   - Spring에서 FastAPI 호출하도록 변경

### Phase 2: AI 서비스 이전 (1-2주)
1. **AI 서비스 이전**
   - AI 채팅 → FastAPI
   - 뉴스 분석 → FastAPI
   - 포트폴리오 분석 → FastAPI

2. **뉴스 서비스 이전**
   - 뉴스 수집 → FastAPI
   - 뉴스 분석 → FastAPI
   - Spring에서 FastAPI 호출하도록 변경

### Phase 3: 스케줄러 이전 (1주)
1. **스케줄러 이전**
   - 주가 업데이트 스케줄러 → FastAPI (APScheduler)
   - 뉴스 수집 스케줄러 → FastAPI
   - 알림 체크 스케줄러 → FastAPI (Spring에 알림 전송)

### Phase 4: 최적화 및 테스트 (1주)
1. **성능 최적화**
   - 비동기 처리 최적화
   - 캐싱 전략 적용
   - API 응답 시간 개선

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

