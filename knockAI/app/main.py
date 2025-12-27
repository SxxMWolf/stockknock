from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.api import stock, ai, news
from app.database import engine, Base

# 데이터베이스 테이블 생성
Base.metadata.create_all(bind=engine)

app = FastAPI(
    title="StocKKnock AI Service",
    description="FastAPI 기반 AI 및 데이터 처리 서비스",
    version="1.0.0"
)

# CORS 설정
app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        "http://localhost:3000",
        "http://localhost:5173",  # Vite 개발 서버
        "http://localhost:8080",
    ],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 라우터 등록
app.include_router(stock.router)
app.include_router(ai.router)
app.include_router(news.router)


@app.get("/")
async def root():
    return {
        "message": "StocKKnock AI Service",
        "version": "1.0.0",
        "status": "running"
    }


@app.get("/health")
async def health():
    return {"status": "healthy"}










