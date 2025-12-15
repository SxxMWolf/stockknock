from sqlalchemy import Column, String, Numeric, BigInteger, DateTime, ForeignKey, Text, Integer, Boolean
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
from app.database import Base
from datetime import datetime


class Stock(Base):
    __tablename__ = "stocks"

    symbol = Column(String(20), primary_key=True)
    name = Column(String(255), nullable=False)
    exchange = Column(String(50))
    country = Column(String(50))
    industry = Column(String(100))
    currency = Column(String(10))
    created_at = Column(DateTime, default=func.now())
    updated_at = Column(DateTime, default=func.now(), onupdate=func.now())


class StockPriceHistory(Base):
    __tablename__ = "stock_price_history"

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    stock_symbol = Column(String(20), ForeignKey("stocks.symbol"), nullable=False)
    price = Column(Numeric(18, 4), nullable=False)
    open = Column(Numeric(18, 4))
    high = Column(Numeric(18, 4))
    low = Column(Numeric(18, 4))
    volume = Column(BigInteger)
    timestamp = Column(DateTime, nullable=False)
    created_at = Column(DateTime, default=func.now())


class News(Base):
    __tablename__ = "news"

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    title = Column(Text, nullable=False)
    content = Column(Text)
    url = Column(Text)
    source = Column(String(255))
    published_at = Column(DateTime)
    created_at = Column(DateTime, default=func.now())


class NewsAnalysis(Base):
    __tablename__ = "news_analyses"

    news_id = Column(BigInteger, ForeignKey("news.id"), primary_key=True)
    summary = Column(Text)
    sentiment = Column(String(20))
    impact_score = Column(Integer)
    ai_comment = Column(Text)
    analyzed_at = Column(DateTime, default=func.now())


class NewsStockRelation(Base):
    __tablename__ = "news_stock_relations"

    news_id = Column(BigInteger, ForeignKey("news.id"), primary_key=True)
    stock_symbol = Column(String(20), ForeignKey("stocks.symbol"), primary_key=True)
    created_at = Column(DateTime, default=func.now())


class AIConversation(Base):
    __tablename__ = "ai_conversation"

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    user_id = Column(BigInteger, nullable=False)
    role = Column(String(10), nullable=False)  # 'user', 'assistant'
    message = Column(Text, nullable=False)
    created_at = Column(DateTime, default=func.now())

