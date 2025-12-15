import asyncio
import httpx
from datetime import datetime, timedelta
from sqlalchemy.orm import Session
from sqlalchemy import select, and_
from app.models import News, NewsAnalysis, NewsStockRelation, Stock
from app.services.ai_service import AIService
import os
from dotenv import load_dotenv
import re

load_dotenv()


class NewsService:
    def __init__(self, db: Session):
        self.db = db
        self.news_api_key = os.getenv("NEWS_API_KEY", "")
        self.ai_service = AIService()

    async def collect_news_from_newsapi(self, query: str):
        """NewsAPI를 통해 뉴스 수집"""
        if not self.news_api_key:
            return

        try:
            async with httpx.AsyncClient(timeout=10.0) as client:
                url = (
                    f"https://newsapi.org/v2/everything"
                    f"?q={query}&language=ko&sortBy=publishedAt&apiKey={self.news_api_key}"
                )
                response = await client.get(url)

                if response.status_code == 200:
                    data = response.json()
                    articles = data.get("articles", [])

                    for article in articles:
                        title = article.get("title")
                        content = article.get("content", "")
                        source = article.get("source", {}).get("name", "")
                        article_url = article.get("url", "")
                        published_at_str = article.get("publishedAt", "")

                        # 중복 체크
                        existing = self.db.execute(
                            select(News).where(
                                News.title.contains(title[:50]) if title else False
                            )
                        ).scalar_one_or_none()

                        if not existing and title:
                            published_at = self._parse_datetime(published_at_str)
                            
                            news = News(
                                title=title,
                                content=content if content else "",
                                source=source,
                                url=article_url,
                                published_at=published_at
                            )
                            
                            self.db.add(news)
                            self.db.commit()

        except Exception as e:
            print(f"NewsAPI 수집 오류: {e}")
            self.db.rollback()

    async def collect_news_for_stock(self, stock_symbol: str):
        """주식 심볼별 뉴스 수집"""
        stock = self.db.execute(
            select(Stock).where(Stock.symbol == stock_symbol)
        ).scalar_one_or_none()

        if not stock:
            return

        query = f"{stock.name} OR {stock_symbol}"
        await self.collect_news_from_newsapi(query)

    async def collect_general_stock_news(self):
        """일반 주식 뉴스 수집"""
        await self.collect_news_from_newsapi("주식 OR 증시 OR 투자")

    async def analyze_news(self, news_id: int) -> dict:
        """뉴스 AI 분석"""
        news = self.db.execute(
            select(News).where(News.id == news_id)
        ).scalar_one_or_none()

        if not news:
            return None

        # 이미 분석이 있으면 반환
        existing_analysis = self.db.execute(
            select(NewsAnalysis).where(NewsAnalysis.news_id == news_id)
        ).scalar_one_or_none()

        if existing_analysis:
            return {
                "summary": existing_analysis.summary,
                "sentiment": existing_analysis.sentiment,
                "impact_score": existing_analysis.impact_score,
                "ai_comment": existing_analysis.ai_comment
            }

        # AI 분석 수행
        news_content = f"{news.title}\n\n{news.content or ''}"
        analysis_result = self.ai_service.analyze_news(news_content)

        # 분석 결과 파싱
        summary = analysis_result[:200] + "..." if len(analysis_result) > 200 else analysis_result
        sentiment = self._determine_sentiment(analysis_result)
        impact_score = self._calculate_impact_score(analysis_result)

        # NewsAnalysis 저장
        analysis = NewsAnalysis(
            news_id=news_id,
            summary=summary,
            sentiment=sentiment,
            impact_score=impact_score,
            ai_comment=analysis_result,
            analyzed_at=datetime.now()
        )

        self.db.add(analysis)
        self.db.commit()

        return {
            "summary": summary,
            "sentiment": sentiment,
            "impact_score": impact_score,
            "ai_comment": analysis_result
        }

    def _determine_sentiment(self, analysis: str) -> str:
        """감정 분석"""
        lower = analysis.lower()
        if any(word in lower for word in ["긍정", "상승", "호재", "긍정적"]):
            return "positive"
        elif any(word in lower for word in ["부정", "하락", "악재", "부정적"]):
            return "negative"
        return "neutral"

    def _calculate_impact_score(self, analysis: str) -> int:
        """영향 점수 계산"""
        # 간단한 점수 계산 (실제로는 더 정교하게)
        score = 5  # 기본값
        
        # 키워드 기반 점수 조정
        lower = analysis.lower()
        if any(word in lower for word in ["큰 영향", "중요", "핵심", "주요"]):
            score += 2
        if any(word in lower for word in ["미미", "작은", "경미"]):
            score -= 2
        
        return max(1, min(10, score))

    def _parse_datetime(self, date_str: str) -> datetime:
        """날짜 문자열 파싱"""
        try:
            # ISO 8601 형식 파싱
            return datetime.fromisoformat(date_str.replace("Z", "+00:00"))
        except:
            return datetime.now()







