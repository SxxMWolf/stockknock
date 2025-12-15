from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from app.database import get_db
from app.services.ai_service import AIService
from app.schemas import AIChatRequest, AIChatResponse, PortfolioAnalysisRequest
from sqlalchemy import select, desc
from app.models import AIConversation
from typing import Optional

router = APIRouter(prefix="/api/ai", tags=["ai"])


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
    
    # AI 응답 생성
    response = ai_service.answer_question_with_context(
        request.question,
        context if context else None,
        len(recent_conversations)
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



