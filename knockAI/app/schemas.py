from pydantic import BaseModel
from typing import Optional
from datetime import datetime
from decimal import Decimal


class StockPriceResponse(BaseModel):
    symbol: str
    price: Decimal
    open: Optional[Decimal] = None
    high: Optional[Decimal] = None
    low: Optional[Decimal] = None
    volume: Optional[int] = None
    timestamp: datetime

    class Config:
        from_attributes = True


class StockPriceUpdateRequest(BaseModel):
    symbol: str


class AIChatRequest(BaseModel):
    question: str
    conversation_type: Optional[str] = None
    user_id: int
    conversation_history: Optional[str] = None


class AIChatResponse(BaseModel):
    response: str
    conversation_type: Optional[str] = None


class NewsAnalysisRequest(BaseModel):
    news_id: int
    news_content: str


class NewsAnalysisResponse(BaseModel):
    summary: str
    sentiment: str
    impact_score: int
    ai_comment: str


class PortfolioAnalysisRequest(BaseModel):
    portfolio_summary: str
    user_investment_style: Optional[str] = None



