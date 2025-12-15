# StockKnock AI Service (FastAPI)

StockKnock의 AI 및 데이터 처리 서비스를 담당하는 FastAPI 애플리케이션입니다.

## 주요 기능

- **주가 서비스**: Yahoo Finance, Alpha Vantage, Twelve Data를 통한 주가 조회 및 업데이트
- **AI 서비스**: OpenAI GPT를 활용한 채팅, 뉴스 분석, 포트폴리오 분석
- **뉴스 서비스**: NewsAPI를 통한 뉴스 수집 및 AI 분석

## 시작하기

### 1. 가상 환경 생성 및 활성화

```bash
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate
```

### 2. 의존성 설치

```bash
pip install -r requirements.txt
```

### 3. 환경 변수 설정

`.env.example`을 참고하여 `.env` 파일을 생성하고 필요한 API 키를 설정하세요.

```bash
cp .env.example .env
# .env 파일 편집
```

### 4. 서버 실행

```bash
uvicorn app.main:app --reload --port 8000
```

서버는 `http://localhost:8000`에서 실행됩니다.

## API 엔드포인트

### 주가 서비스

- `GET /api/stock/{symbol}/price` - 현재 주가 조회
- `POST /api/stock/{symbol}/update` - 주가 업데이트
- `POST /api/stock/update-all` - 모든 주식 가격 업데이트

### AI 서비스

- `POST /api/ai/chat` - AI 채팅 (문맥 유지)
- `POST /api/ai/analyze-portfolio` - 포트폴리오 AI 분석

### 뉴스 서비스

- `POST /api/news/collect/{stock_symbol}` - 주식 심볼별 뉴스 수집
- `POST /api/news/collect-general` - 일반 주식 뉴스 수집
- `POST /api/news/analyze/{news_id}` - 뉴스 AI 분석

## 데이터베이스

PostgreSQL 데이터베이스를 Spring Boot와 공유합니다. 동일한 스키마를 사용합니다.

## 개발

### 코드 구조

```
knockAI/
├── app/
│   ├── main.py           # FastAPI 앱 진입점
│   ├── database.py       # 데이터베이스 연결
│   ├── models.py         # SQLAlchemy 모델
│   ├── schemas.py        # Pydantic 스키마
│   ├── api/              # API 라우터
│   │   ├── stock.py
│   │   ├── ai.py
│   │   └── news.py
│   └── services/         # 비즈니스 로직
│       ├── stock_service.py
│       ├── ai_service.py
│       └── news_service.py
├── requirements.txt
└── README.md
```







