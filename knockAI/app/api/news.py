from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session
from sqlalchemy import select, desc
from datetime import datetime, timedelta
from typing import List, Optional
from app.database import get_db
from app.services.news_service import NewsService
from app.schemas import NewsAnalysisRequest, NewsAnalysisResponse
from app.models import News, NewsAnalysis, NewsStockRelation

router = APIRouter(prefix="/api/news", tags=["news"])


@router.get("/recent")
async def get_recent_news(
    days: int = Query(7, ge=1, le=30),
    db: Session = Depends(get_db)
):
    """최근 뉴스 조회"""
    cutoff_date = datetime.now() - timedelta(days=days)
    
    news_list = db.execute(
        select(News)
        .where(News.published_at >= cutoff_date)
        .order_by(desc(News.published_at))
        .limit(100)
    ).scalars().all()
    
    result = []
    for news in news_list:
        # 관련 종목 심볼 가져오기
        related_stocks = db.execute(
            select(NewsStockRelation.stock_symbol)
            .where(NewsStockRelation.news_id == news.id)
        ).scalars().all()
        
        # 분석 결과 가져오기
        analysis = db.execute(
            select(NewsAnalysis)
            .where(NewsAnalysis.news_id == news.id)
        ).scalar_one_or_none()
        
        news_dict = {
            "id": news.id,
            "title": news.title,
            "content": news.content or "",
            "source": news.source or "",
            "url": news.url or "",
            "published_at": news.published_at.isoformat() if news.published_at else None,
            "related_stock_symbols": list(related_stocks)
        }
        
        if analysis:
            news_dict["analysis"] = {
                "summary": analysis.summary,
                "sentiment": analysis.sentiment,
                "impact_score": analysis.impact_score,
                "ai_comment": analysis.ai_comment
            }
        
        result.append(news_dict)
    
    return result


@router.get("/{news_id}")
async def get_news_by_id(
    news_id: int,
    db: Session = Depends(get_db)
):
    """뉴스 상세 조회"""
    news = db.execute(
        select(News).where(News.id == news_id)
    ).scalar_one_or_none()
    
    if not news:
        raise HTTPException(status_code=404, detail="뉴스를 찾을 수 없습니다.")
    
    # 관련 종목 심볼 가져오기
    related_stocks = db.execute(
        select(NewsStockRelation.stock_symbol)
        .where(NewsStockRelation.news_id == news.id)
    ).scalars().all()
    
    # 분석 결과 가져오기
    analysis = db.execute(
        select(NewsAnalysis)
        .where(NewsAnalysis.news_id == news.id)
    ).scalar_one_or_none()
    
    result = {
        "id": news.id,
        "title": news.title,
        "content": news.content or "",
        "source": news.source or "",
        "url": news.url or "",
        "published_at": news.published_at.isoformat() if news.published_at else None,
        "related_stock_symbols": list(related_stocks)
    }
    
    if analysis:
        result["analysis"] = {
            "summary": analysis.summary,
            "sentiment": analysis.sentiment,
            "impact_score": analysis.impact_score,
            "ai_comment": analysis.ai_comment
        }
    
    return result


@router.post("/collect/{stock_symbol}")
async def collect_news_for_stock(
    stock_symbol: str,
    db: Session = Depends(get_db)
):
    """주식 심볼별 뉴스 수집"""
    service = NewsService(db)
    await service.collect_news_for_stock(stock_symbol)
    
    return {"message": f"{stock_symbol} 뉴스 수집 완료"}


@router.post("/collect-general")
async def collect_general_news(
    db: Session = Depends(get_db)
):
    """일반 주식 뉴스 수집"""
    service = NewsService(db)
    await service.collect_general_stock_news()
    
    return {"message": "일반 주식 뉴스 수집 완료"}


@router.post("/analyze/{news_id}", response_model=NewsAnalysisResponse)
async def analyze_news(
    news_id: int,
    db: Session = Depends(get_db)
):
    """뉴스 AI 분석"""
    service = NewsService(db)
    analysis = await service.analyze_news(news_id)
    
    if not analysis:
        raise HTTPException(status_code=404, detail="뉴스를 찾을 수 없습니다.")
    
    return NewsAnalysisResponse(**analysis)



