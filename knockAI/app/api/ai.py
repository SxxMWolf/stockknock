from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from app.database import get_db
from app.services.ai_service import AIService
from app.schemas import AIChatRequest, AIChatResponse, PortfolioAnalysisRequest
from sqlalchemy import select, desc, or_
from app.models import AIConversation, News, NewsAnalysis, Stock
from datetime import datetime, timedelta
import re

router = APIRouter(prefix="/api/ai", tags=["ai"])


def extract_keywords_from_question(question: str, db: Session) -> list[str]:
    """질문에서 종목명이나 키워드 추출"""
    keywords = []
    
    # 주요 종목명 패턴 (한국 주식)
    korean_stocks = ["삼성전자", "SK하이닉스", "LG전자", "현대차", "기아", "네이버", "카카오", 
                     "셀트리온", "삼성SDI", "LG화학", "POSCO", "KB금융", "신한지주", "하나금융"]
    
    # 질문에서 종목명 찾기
    for stock_name in korean_stocks:
        if stock_name in question:
            keywords.append(stock_name)
            # DB에서 심볼도 찾기
            stock = db.execute(
                select(Stock).where(Stock.name.contains(stock_name))
            ).scalar_one_or_none()
            if stock:
                keywords.append(stock.symbol)
    
    # 영어 종목명/심볼 패턴 (예: AAPL, Apple, TSLA 등)
    english_patterns = re.findall(r'\b[A-Z]{2,5}\b', question.upper())
    for pattern in english_patterns:
        if len(pattern) >= 2 and len(pattern) <= 5:
            keywords.append(pattern)
    
    # 일반 키워드 (섹터, 산업 등)
    sector_keywords = ["반도체", "전자", "자동차", "금융", "바이오", "IT", "AI", "배터리", "2차전지"]
    for keyword in sector_keywords:
        if keyword in question:
            keywords.append(keyword)
    
    return list(set(keywords))  # 중복 제거


def get_relevant_news_summary(keywords: list[str], db: Session, limit: int = 5) -> str:
    """관련 뉴스를 가져와서 문단 형식의 상황 요약으로 변환"""
    if not keywords:
        return ""
    
    # 최근 7일 이내 뉴스만 검색
    cutoff_date = datetime.now() - timedelta(days=7)
    
    # 키워드로 뉴스 검색 (제목이나 내용에 포함)
    conditions = []
    for keyword in keywords:
        conditions.append(News.title.contains(keyword))
        conditions.append(News.content.contains(keyword))
    
    if not conditions:
        return ""
    
    news_list = db.execute(
        select(News)
        .where(
            or_(*conditions),
            News.published_at >= cutoff_date
        )
        .order_by(desc(News.published_at))
        .limit(limit)
    ).scalars().all()
    
    if not news_list:
        return ""
    
    # 뉴스 내용을 수집하여 문단 형식으로 변환
    news_points = []
    for news in news_list:
        # NewsAnalysis의 summary가 있으면 사용
        analysis = db.execute(
            select(NewsAnalysis).where(NewsAnalysis.news_id == news.id)
        ).scalar_one_or_none()
        
        if analysis and analysis.summary:
            point = analysis.summary.strip()
        else:
            # 없으면 title + content 일부 사용
            content_preview = (news.content or "").strip()[:120] if news.content else ""
            point = news.title.strip()
            if content_preview:
                point += f" {content_preview}"
        
        # 각 포인트가 너무 길면 자르기
        if len(point) > 200:
            point = point[:200] + "..."
        
        news_points.append(point)
    
    # 키워드 기반으로 주제 파악 (여러 키워드 조합)
    if len(keywords) >= 2:
        main_keyword = "·".join(keywords[:2])
    elif len(keywords) == 1:
        main_keyword = keywords[0]
    else:
        main_keyword = "관련"
    
    # 문단 형식으로 자연스럽게 변환
    # 여러 뉴스를 하나의 문단으로 연결
    if len(news_points) == 1:
        summary_paragraph = f"최근 {main_keyword} 관련 뉴스 흐름을 보면, {news_points[0]}"
    elif len(news_points) == 2:
        summary_paragraph = f"최근 {main_keyword} 관련 뉴스 흐름을 종합하면, {news_points[0]}. 또한 {news_points[1]}"
    else:
        # 3개 이상: 첫 번째를 주제로, 나머지를 연결
        first_news = news_points[0]
        remaining_news = news_points[1:]
        
        # 나머지 뉴스를 자연스럽게 연결
        if len(remaining_news) == 1:
            remaining_text = remaining_news[0]
        elif len(remaining_news) == 2:
            remaining_text = f"{remaining_news[0]}. 또한 {remaining_news[1]}"
        else:
            # 3개 이상이면 첫 2개만 사용
            remaining_text = f"{remaining_news[0]}. 또한 {remaining_news[1]}"
        
        summary_paragraph = f"최근 {main_keyword} 관련 뉴스 흐름을 종합하면, {first_news}. {remaining_text}"
    
    # 방향성 힌트 추가 (리서치 느낌 완성)
    summary_paragraph += " 전반적으로 시장은 단기 회복 기대와 중장기 불확실성이 공존하는 국면으로 해석된다."
    
    # 문단 길이 제한 (너무 길면 자르기)
    if len(summary_paragraph) > 800:
        summary_paragraph = summary_paragraph[:800] + "..."
    
    return summary_paragraph


@router.post("/chat", response_model=AIChatResponse)
async def chat(
    request: AIChatRequest,
    db: Session = Depends(get_db)
):
    """AI 채팅 (문맥 유지)"""
    ai_service = AIService()
    
    # 최근 대화 기록 가져오기
    recent_conversations = db.execute(
        select(AIConversation)
        .where(AIConversation.user_id == request.user_id)
        .order_by(desc(AIConversation.created_at))
        .limit(5)
    ).scalars().all()
    
    # 대화 문맥 구성
    context = ""
    if recent_conversations:
        context_lines = []
        for conv in reversed(recent_conversations):
            role = "사용자" if conv.role == "user" else "AI"
            context_lines.append(f"{role}: {conv.message}")
        context = "\n".join(context_lines)
    
    # 질문에서 키워드 추출 및 관련 뉴스 가져오기
    keywords = extract_keywords_from_question(request.question, db)
    news_context = get_relevant_news_summary(keywords, db) if keywords else None
    
    # AI 응답 생성
    response = ai_service.answer_question_with_context(
        request.question,
        context if context else None,
        len(recent_conversations),
        news_context=news_context
    )
    
    # 대화 기록 저장 (사용자 질문)
    user_message = AIConversation(
        user_id=request.user_id,
        role="user",
        message=request.question
    )
    db.add(user_message)
    
    # AI 응답 저장
    ai_message = AIConversation(
        user_id=request.user_id,
        role="assistant",
        message=response
    )
    db.add(ai_message)
    db.commit()
    
    return AIChatResponse(
        response=response,
        conversation_type=request.conversation_type
    )


@router.post("/analyze-portfolio")
async def analyze_portfolio(
    request: PortfolioAnalysisRequest,
    db: Session = Depends(get_db)
):
    """포트폴리오 AI 분석"""
    ai_service = AIService()
    analysis = ai_service.analyze_portfolio(
        request.portfolio_summary, 
        request.user_investment_style
    )
    
    return {"analysis": analysis}

