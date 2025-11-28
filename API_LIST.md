# StockKnock API 목록

## 기본 정보
- Base URL: `http://localhost:8080/api`
- 인증: JWT Token (Bearer Token)
- Content-Type: `application/json`

---

## 1. 인증 (Authentication)

### 1.1 회원가입
- **URL**: `POST /api/auth/register`
- **인증**: 불필요
- **Request Body**:
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```
- **Response**:
```json
{
  "token": "jwt_token_string",
  "email": "user@example.com",
  "name": null,
  "userId": 1
}
```

### 1.2 로그인
- **URL**: `POST /api/auth/login`
- **인증**: 불필요
- **Request Body**:
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```
- **Response**: 회원가입과 동일

---

## 2. 주식 정보 (Stock)

### 2.1 종목 코드로 조회
- **URL**: `GET /api/stocks/symbol/{symbol}`
- **인증**: 불필요
- **Response**:
```json
{
  "id": 1,
  "symbol": "AAPL",
  "name": "Apple Inc.",
  "exchange": "NASDAQ",
  "country": "US",
  "currentPrice": 175.50,
  "previousClose": 174.20,
  "dayHigh": 176.00,
  "dayLow": 174.50,
  "volume": 50000000,
  "marketCap": 2800000000000,
  "peRatio": 30.5,
  "dividendYield": 0.5
}
```

### 2.2 종목 검색
- **URL**: `GET /api/stocks/search?keyword={keyword}`
- **인증**: 불필요
- **Response**: StockDto 배열

### 2.3 국가별 주식 조회
- **URL**: `GET /api/stocks/country/{country}`
- **인증**: 불필요
- **Response**: StockDto 배열

### 2.4 산업별 주식 조회
- **URL**: `GET /api/stocks/industry/{industryId}`
- **인증**: 불필요
- **Response**: StockDto 배열

---

## 3. 포트폴리오 (Portfolio)

### 3.1 포트폴리오 조회
- **URL**: `GET /api/portfolio`
- **인증**: 필요 (Bearer Token)
- **Headers**: `Authorization: Bearer {token}`
- **Response**: PortfolioDto 배열
```json
[
  {
    "id": 1,
    "stock": {
      "id": 1,
      "symbol": "AAPL",
      "name": "Apple Inc.",
      ...
    },
    "quantity": 10,
    "averagePrice": 170.00,
    "currentPrice": 175.50,
    "totalValue": 1755.00,
    "profitLoss": 55.00,
    "profitLossRate": 3.24
  }
]
```

### 3.2 포트폴리오에 추가
- **URL**: `POST /api/portfolio?stockSymbol={symbol}&quantity={qty}&averagePrice={price}`
- **인증**: 필요
- **Response**: PortfolioDto

### 3.3 포트폴리오 수정
- **URL**: `PUT /api/portfolio/{portfolioId}?quantity={qty}&averagePrice={price}`
- **인증**: 필요
- **Response**: PortfolioDto

### 3.4 포트폴리오에서 삭제
- **URL**: `DELETE /api/portfolio/{portfolioId}`
- **인증**: 필요

---

## 4. 관심 종목 (Watchlist)

### 4.1 관심 종목 조회
- **URL**: `GET /api/watchlist`
- **인증**: 필요
- **Response**: StockDto 배열

### 4.2 관심 종목 추가
- **URL**: `POST /api/watchlist/{stockSymbol}`
- **인증**: 필요

### 4.3 관심 종목 제거
- **URL**: `DELETE /api/watchlist/{stockSymbol}`
- **인증**: 필요

---

## 5. 뉴스 (News)

### 5.1 최근 뉴스 조회
- **URL**: `GET /api/news/recent?days={days}`
- **인증**: 불필요
- **기본값**: days=7
- **Response**: NewsDto 배열
```json
[
  {
    "id": 1,
    "title": "뉴스 제목",
    "content": "뉴스 내용...",
    "source": "출처",
    "url": "https://...",
    "publishedAt": "2024-01-01T00:00:00",
    "relatedStockSymbols": ["AAPL", "MSFT"],
    "analysis": {
      "summary": "AI 요약",
      "impactAnalysis": "주가 영향 분석",
      "sentiment": "POSITIVE",
      "impactScore": 7
    }
  }
]
```

### 5.2 뉴스 상세 조회
- **URL**: `GET /api/news/{newsId}`
- **인증**: 불필요
- **Response**: NewsDto

### 5.3 뉴스 AI 분석
- **URL**: `POST /api/news/{newsId}/analyze`
- **인증**: 불필요
- **Response**: NewsAnalysisDto

---

## 6. AI 채팅 (AI Chat)

### 6.1 AI와 대화
- **URL**: `POST /api/ai/chat`
- **인증**: 필요
- **Request Body**:
```json
{
  "question": "애플 주식의 전망은 어때요?",
  "conversationType": "STOCK_PREDICTION"
}
```
- **Response**:
```json
{
  "response": "AI 응답 내용...",
  "conversationType": "STOCK_PREDICTION"
}
```

---

## 7. 가격 알림 (Price Alert)

### 7.1 알림 목록 조회
- **URL**: `GET /api/alerts`
- **인증**: 필요
- **Response**: PriceAlert 배열

### 7.2 알림 생성
- **URL**: `POST /api/alerts?stockSymbol={symbol}&alertType={type}&targetPrice={price}`
- **인증**: 필요
- **Alert Types**: `TARGET_PRICE`, `STOP_LOSS`, `PERCENTAGE_CHANGE`

### 7.3 알림 삭제
- **URL**: `DELETE /api/alerts/{alertId}`
- **인증**: 필요

---

## 에러 응답

### 401 Unauthorized
```json
{
  "error": "인증이 필요합니다"
}
```

### 404 Not Found
```json
{
  "error": "리소스를 찾을 수 없습니다"
}
```

### 400 Bad Request
```json
{
  "error": "잘못된 요청입니다"
}
```

---

## 인증 사용 예시

모든 인증이 필요한 API는 헤더에 다음을 포함해야 합니다:

```
Authorization: Bearer {jwt_token}
```

예시:
```bash
curl -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  http://localhost:8080/api/portfolio
```

---

## 주의사항

1. JWT 토큰은 로그인/회원가입 시 발급되며, 클라이언트에 저장해야 합니다.
2. 토큰 만료 시 다시 로그인해야 합니다.
3. 모든 가격은 원화 기준입니다 (필요시 환율 적용).
4. 날짜는 ISO 8601 형식을 사용합니다.

