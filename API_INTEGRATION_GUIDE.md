# StockKnock API 연동 가이드

이 문서는 StockKnock 프로젝트에서 사용하는 외부 API들의 연동 방법과 설정을 안내합니다.

---

## 📋 목차

1. [주식 가격 API](#1-주식-가격-api)
2. [뉴스 수집 API](#2-뉴스-수집-api)
3. [AI 서비스 API](#3-ai-서비스-api)
4. [알림 서비스 API](#4-알림-서비스-api)
5. [환경 변수 설정](#5-환경-변수-설정)
6. [API 제한 및 비용](#6-api-제한-및-비용)

---

## 1. 주식 가격 API

### 1.1 Yahoo Finance API (무료, 추천)

**특징:**
- 무료, API 키 불필요
- 실시간 가격 정보 제공
- 제한: 초당 2회 요청

**연동 방법:**
```properties
# application.properties
stock.api.yahoo-finance.enabled=true
```

**사용 예시:**
- URL: `https://query1.finance.yahoo.com/v8/finance/chart/{SYMBOL}?interval=1d&range=1d`
- 심볼 예시: `AAPL`, `MSFT`, `005930` (삼성전자)

**장점:**
- 무료
- API 키 불필요
- 빠른 응답

**단점:**
- 공식 API가 아니므로 변경 가능성 있음
- 제한이 있음

---

### 1.2 Alpha Vantage API

**특징:**
- 무료 플랜: 일일 25회 요청, 분당 5회 요청
- 유료 플랜: 월 $49.99부터

**가입 및 API 키 발급:**
1. https://www.alphavantage.co/support/#api-key 방문
2. 이메일 입력 후 API 키 발급
3. 무료 플랜은 즉시 사용 가능

**연동 방법:**
```properties
# application.properties
stock.api.alpha-vantage.key=YOUR_API_KEY_HERE
```

**환경 변수:**
```bash
export ALPHA_VANTAGE_API_KEY=your-api-key-here
```

**API 엔드포인트:**
- 실시간 가격: `https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol={SYMBOL}&apikey={API_KEY}`
- 과거 데이터: `https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol={SYMBOL}&apikey={API_KEY}`

**제한:**
- 무료: 일일 25회, 분당 5회
- 유료: 제한 없음

---

### 1.3 Twelve Data API

**특징:**
- 무료 플랜: 일일 800회 요청
- 유료 플랜: 월 $9.99부터

**가입 및 API 키 발급:**
1. https://twelvedata.com/ 회원가입
2. 대시보드에서 API 키 발급

**연동 방법:**
```properties
# application.properties
stock.api.twelve-data.key=YOUR_API_KEY_HERE
```

**환경 변수:**
```bash
export TWELVE_DATA_API_KEY=your-api-key-here
```

**API 엔드포인트:**
- 실시간 가격: `https://api.twelvedata.com/price?symbol={SYMBOL}&apikey={API_KEY}`
- 과거 데이터: `https://api.twelvedata.com/time_series?symbol={SYMBOL}&interval=1day&apikey={API_KEY}`

**제한:**
- 무료: 일일 800회
- 유료: 제한 없음

---

## 2. 뉴스 수집 API

### 2.1 NewsAPI

**특징:**
- 무료 플랜: 일일 100회 요청
- 개발자 플랜: 월 $449부터

**가입 및 API 키 발급:**
1. https://newsapi.org/register 회원가입
2. 대시보드에서 API 키 발급

**연동 방법:**
```properties
# application.properties
news.api.newsapi.key=YOUR_API_KEY_HERE
news.api.enabled=true
```

**환경 변수:**
```bash
export NEWS_API_KEY=your-api-key-here
```

**API 엔드포인트:**
- 뉴스 검색: `https://newsapi.org/v2/everything?q={QUERY}&language=ko&sortBy=publishedAt&apiKey={API_KEY}`
- 헤드라인: `https://newsapi.org/v2/top-headlines?country=kr&apiKey={API_KEY}`

**제한:**
- 무료: 일일 100회
- 유료: 제한 없음

**사용 예시:**
```java
// 주식 관련 뉴스 검색
collectNewsFromNewsAPI("주식 OR 증시 OR 투자");

// 특정 종목 뉴스 검색
collectNewsFromNewsAPI("삼성전자 OR 005930");
```

---

### 2.2 RSS 피드 (무료 대안)

**특징:**
- 완전 무료
- API 키 불필요
- RSS 피드 파싱 필요

**주요 뉴스 사이트 RSS:**
- 네이버 증권: `https://finance.naver.com/news/news_list.naver?mode=RSS`
- 다음 증권: `https://finance.daum.net/news/rss`
- 연합뉴스: `https://www.yna.co.kr/rss/economy.xml`

**구현 예시:**
```java
// RSS 피드 파싱 라이브러리 필요 (예: Rome)
// https://github.com/rometools/rome
```

---

## 3. AI 서비스 API

### 3.1 OpenAI GPT-4 API

**특징:**
- GPT-4 모델 사용
- 뉴스 분석, 포트폴리오 분석, 채팅 기능

**가입 및 API 키 발급:**
1. https://platform.openai.com/ 회원가입
2. API 키 생성: https://platform.openai.com/api-keys
3. 결제 정보 등록 (크레딧 필요)

**연동 방법:**
```properties
# application.properties
openai.api.key=YOUR_API_KEY_HERE
```

**환경 변수:**
```bash
export OPENAI_API_KEY=sk-your-api-key-here
```

**비용:**
- GPT-4: 입력 $0.03/1K 토큰, 출력 $0.06/1K 토큰
- GPT-3.5-turbo: 입력 $0.0015/1K 토큰, 출력 $0.002/1K 토큰 (더 저렴)

**사용 예시:**
```java
// 뉴스 분석
aiService.analyzeNews(newsContent);

// 포트폴리오 분석
aiService.analyzePortfolio(portfolioSummary, investmentStyle);

// 질문 답변
aiService.answerQuestionWithContext(question, conversationHistory, historyCount);
```

**비용 절감 팁:**
- GPT-3.5-turbo 사용 고려 (더 저렴)
- 토큰 수 제한 (`maxTokens` 설정)
- 캐싱 활용 (이미 분석한 뉴스는 재분석하지 않음)

---

## 4. 알림 서비스 API

### 4.1 이메일 알림 (Spring Mail)

**특징:**
- Gmail, Outlook 등 SMTP 서버 사용
- 무료 (Gmail 기준)

**Gmail 설정:**
1. Google 계정 설정에서 "앱 비밀번호" 생성
2. 2단계 인증 활성화 필요

**연동 방법:**
```properties
# application.properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

notification.email.enabled=true
```

**환경 변수:**
```bash
export SPRING_MAIL_USERNAME=your-email@gmail.com
export SPRING_MAIL_PASSWORD=your-app-password
```

**의존성 추가 (build.gradle):**
```gradle
implementation 'org.springframework.boot:spring-boot-starter-mail'
```

---

### 4.2 SMS 알림 (Twilio)

**특징:**
- 유료 서비스
- 월 $0.0075/SMS (한국 기준)

**가입 및 설정:**
1. https://www.twilio.com/ 회원가입
2. 전화번호 구매
3. API 키 발급

**연동 방법:**
```properties
# application.properties
twilio.account.sid=YOUR_ACCOUNT_SID
twilio.auth.token=YOUR_AUTH_TOKEN
twilio.phone.number=YOUR_PHONE_NUMBER
```

**환경 변수:**
```bash
export TWILIO_ACCOUNT_SID=your-account-sid
export TWILIO_AUTH_TOKEN=your-auth-token
```

**의존성 추가:**
```gradle
implementation 'com.twilio.sdk:twilio:9.0.0'
```

---

### 4.3 푸시 알림 (Firebase Cloud Messaging)

**특징:**
- 무료 (일일 100만 건까지)
- Android/iOS 지원

**설정 방법:**
1. https://console.firebase.google.com/ 프로젝트 생성
2. FCM 서버 키 발급
3. 클라이언트 SDK 설정

**연동 방법:**
```properties
# application.properties
fcm.server.key=YOUR_SERVER_KEY
```

---

## 5. 환경 변수 설정

### 5.1 application.properties 예시

```properties
# 주식 가격 API
stock.api.yahoo-finance.enabled=true
stock.api.alpha-vantage.key=${ALPHA_VANTAGE_API_KEY:}
stock.api.twelve-data.key=${TWELVE_DATA_API_KEY:}

# 뉴스 API
news.api.newsapi.key=${NEWS_API_KEY:}
news.api.enabled=true

# OpenAI API
openai.api.key=${OPENAI_API_KEY:your-api-key-here}

# 이메일 알림
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=${SPRING_MAIL_USERNAME:}
spring.mail.password=${SPRING_MAIL_PASSWORD:}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
notification.email.enabled=${NOTIFICATION_EMAIL_ENABLED:false}

# JWT
jwt.secret=${JWT_SECRET:stockknock-secret-key-change-in-production}
jwt.expiration=86400000
```

### 5.2 .env 파일 (로컬 개발)

```bash
# 주식 가격 API
ALPHA_VANTAGE_API_KEY=your-alpha-vantage-key
TWELVE_DATA_API_KEY=your-twelve-data-key

# 뉴스 API
NEWS_API_KEY=your-newsapi-key

# OpenAI
OPENAI_API_KEY=sk-your-openai-key

# 이메일
SPRING_MAIL_USERNAME=your-email@gmail.com
SPRING_MAIL_PASSWORD=your-app-password
NOTIFICATION_EMAIL_ENABLED=true

# JWT
JWT_SECRET=your-secret-key-here
```

---

## 6. API 제한 및 비용

### 6.1 무료 플랜 비교

| API | 일일 제한 | 분당 제한 | 비고 |
|-----|----------|----------|------|
| Yahoo Finance | 제한 없음 | 2회/초 | 공식 API 아님 |
| Alpha Vantage | 25회 | 5회 | 무료 플랜 |
| Twelve Data | 800회 | - | 무료 플랜 |
| NewsAPI | 100회 | - | 무료 플랜 |
| OpenAI | - | - | 사용량 기반 과금 |

### 6.2 비용 추정 (월간)

**소규모 사용 (개인 프로젝트):**
- Yahoo Finance: 무료
- NewsAPI: 무료 (일일 100회)
- OpenAI: 약 $5-10 (GPT-3.5-turbo 사용 시)
- **총계: 약 $5-10/월**

**중규모 사용:**
- Alpha Vantage: 무료 또는 $49.99/월
- NewsAPI: $449/월
- OpenAI: 약 $20-50/월
- **총계: 약 $500-550/월**

### 6.3 최적화 전략

1. **캐싱 활용**
   - 이미 분석한 뉴스는 재분석하지 않음
   - 주식 가격은 1분마다 업데이트 (필요시 조정)

2. **API 우선순위**
   - Yahoo Finance (무료) → Alpha Vantage → Twelve Data
   - 무료 API 우선 사용

3. **요청 빈도 조절**
   - 스케줄러 간격 조정
   - API 제한 고려

4. **모델 선택**
   - GPT-3.5-turbo 사용 (GPT-4보다 저렴)
   - `maxTokens` 제한 설정

---

## 7. 구현된 기능 체크리스트

### ✅ 완료된 기능

- [x] 가격 알림 스케줄러 (10초마다 체크)
- [x] 실시간 가격 업데이트 스케줄러 (1분마다)
- [x] 뉴스 크롤러 서비스 (NewsAPI 연동)
- [x] 뉴스-종목 연관 분석 (AI 기반)
- [x] AI Chat 문맥 유지 (최근 5개 대화)
- [x] AI 포트폴리오 분석
- [x] Validation 추가
- [x] 포트폴리오 수정/삭제 UI

### 🔄 추가 구현 가능한 기능

- [ ] 실적 발표 캘린더 (Alpha Vantage, Finnhub)
- [ ] 월간/주간 포트폴리오 리포트 (PDF 생성)
- [ ] AI 기반 종목 추천 엔진
- [ ] 시장 심리 지수 (Sentiment Index)
- [ ] 주가 예측 ML 모델
- [ ] 산업군 분석 기능
- [ ] TradingView 차트 연동

---

## 8. 문제 해결

### 8.1 API 키 오류

**증상:** `401 Unauthorized` 또는 `Invalid API Key`

**해결:**
1. API 키가 올바른지 확인
2. 환경 변수가 제대로 설정되었는지 확인
3. API 키에 충분한 크레딧이 있는지 확인

### 8.2 API 제한 초과

**증상:** `429 Too Many Requests`

**해결:**
1. 요청 빈도 줄이기 (스케줄러 간격 조정)
2. 캐싱 활용
3. 유료 플랜으로 업그레이드

### 8.3 이메일 발송 실패

**증상:** `MailException`

**해결:**
1. Gmail의 경우 "앱 비밀번호" 사용 확인
2. 2단계 인증 활성화 확인
3. SMTP 설정 확인

---

## 9. 참고 자료

- [Yahoo Finance API (비공식)](https://github.com/ranaroussi/yfinance)
- [Alpha Vantage 문서](https://www.alphavantage.co/documentation/)
- [Twelve Data 문서](https://twelvedata.com/docs)
- [NewsAPI 문서](https://newsapi.org/docs)
- [OpenAI API 문서](https://platform.openai.com/docs)
- [Spring Mail 문서](https://spring.io/guides/gs/sending-email/)

---

## 10. 문의 및 지원

문제가 발생하거나 추가 기능이 필요하시면 이슈를 등록해주세요.

