import asyncio
import httpx
from decimal import Decimal
from datetime import datetime
from typing import Optional
from sqlalchemy.orm import Session
from sqlalchemy import select
from app.models import Stock, StockPriceHistory
import os
from dotenv import load_dotenv
import yfinance as yf

load_dotenv()


class StockService:
    def __init__(self, db: Session):
        self.db = db
        self.alpha_vantage_key = os.getenv("ALPHA_VANTAGE_API_KEY", "")
        self.twelve_data_key = os.getenv("TWELVE_DATA_API_KEY", "")
        self.yahoo_finance_enabled = os.getenv("YAHOO_FINANCE_ENABLED", "true").lower() == "true"

    async def get_current_price(self, symbol: str) -> Optional[Decimal]:
        """현재 주가 조회 (Yahoo Finance 우선)"""
        try:
            if self.yahoo_finance_enabled:
                price = await self._get_price_from_yahoo_finance(symbol)
                if price:
                    return price

            if self.alpha_vantage_key:
                price = await self._get_price_from_alpha_vantage(symbol)
                if price:
                    return price

            if self.twelve_data_key:
                price = await self._get_price_from_twelve_data(symbol)
                if price:
                    return price

            return None
        except Exception as e:
            print(f"주가 조회 오류: {symbol} - {e}")
            return None

    async def _get_price_from_yahoo_finance(self, symbol: str) -> Optional[Decimal]:
        """Yahoo Finance API를 통한 주가 조회"""
        try:
            # yfinance 라이브러리 사용
            ticker = yf.Ticker(symbol)
            data = ticker.history(period="1d")
            
            if not data.empty:
                latest = data.iloc[-1]
                return Decimal(str(latest['Close']))
            
            # 대안: 직접 API 호출
            async with httpx.AsyncClient(timeout=10.0) as client:
                url = f"https://query1.finance.yahoo.com/v8/finance/chart/{symbol}?interval=1d&range=1d"
                response = await client.get(url)
                
                if response.status_code == 200:
                    data = response.json()
                    if "chart" in data and "result" in data["chart"]:
                        result = data["chart"]["result"]
                        if result and len(result) > 0:
                            meta = result[0].get("meta", {})
                            price = meta.get("regularMarketPrice")
                            if price:
                                return Decimal(str(price))
            
            return None
        except Exception as e:
            print(f"Yahoo Finance API 오류: {symbol} - {e}")
            return None

    async def _get_price_from_alpha_vantage(self, symbol: str) -> Optional[Decimal]:
        """Alpha Vantage API를 통한 주가 조회"""
        if not self.alpha_vantage_key:
            return None

        try:
            async with httpx.AsyncClient(timeout=10.0) as client:
                url = f"https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol={symbol}&apikey={self.alpha_vantage_key}"
                response = await client.get(url)
                
                if response.status_code == 200:
                    data = response.json()
                    if "Global Quote" in data:
                        quote = data["Global Quote"]
                        price = quote.get("05. price")
                        if price:
                            return Decimal(price)
            
            return None
        except Exception as e:
            print(f"Alpha Vantage API 오류: {symbol} - {e}")
            return None

    async def _get_price_from_twelve_data(self, symbol: str) -> Optional[Decimal]:
        """Twelve Data API를 통한 주가 조회"""
        if not self.twelve_data_key:
            return None

        try:
            async with httpx.AsyncClient(timeout=10.0) as client:
                url = f"https://api.twelvedata.com/price?symbol={symbol}&apikey={self.twelve_data_key}"
                response = await client.get(url)
                
                if response.status_code == 200:
                    data = response.json()
                    price = data.get("price")
                    if price:
                        return Decimal(str(price))
            
            return None
        except Exception as e:
            print(f"Twelve Data API 오류: {symbol} - {e}")
            return None

    async def update_stock_price(self, symbol: str) -> bool:
        """주가 업데이트 및 히스토리 저장"""
        try:
            # Stock 존재 확인
            stock = self.db.execute(
                select(Stock).where(Stock.symbol == symbol)
            ).scalar_one_or_none()
            
            if not stock:
                return False

            # 현재 가격 조회
            price = await self.get_current_price(symbol)
            if not price:
                return False

            # Yahoo Finance에서 상세 정보 가져오기
            async with httpx.AsyncClient(timeout=10.0) as client:
                url = f"https://query1.finance.yahoo.com/v8/finance/chart/{symbol}?interval=1d&range=1d"
                response = await client.get(url)
                
                if response.status_code == 200:
                    data = response.json()
                    if "chart" in data and "result" in data["chart"]:
                        result = data["chart"]["result"]
                        if result and len(result) > 0:
                            meta = result[0].get("meta", {})
                            
                            # StockPriceHistory 저장
                            price_history = StockPriceHistory(
                                stock_symbol=symbol,
                                price=price,
                                open=Decimal(str(meta.get("previousClose", 0))) if meta.get("previousClose") else None,
                                high=Decimal(str(meta.get("regularMarketDayHigh", 0))) if meta.get("regularMarketDayHigh") else None,
                                low=Decimal(str(meta.get("regularMarketDayLow", 0))) if meta.get("regularMarketDayLow") else None,
                                volume=int(meta.get("regularMarketVolume", 0)) if meta.get("regularMarketVolume") else None,
                                timestamp=datetime.now()
                            )
                            
                            self.db.add(price_history)
                            self.db.commit()
                            return True
            
            return False
        except Exception as e:
            print(f"주가 업데이트 오류: {symbol} - {e}")
            self.db.rollback()
            return False

    async def update_all_stock_prices(self):
        """모든 주식 가격 업데이트"""
        stocks = self.db.execute(select(Stock)).scalars().all()
        
        for stock in stocks:
            try:
                await self.update_stock_price(stock.symbol)
                # API 제한 방지
                await asyncio.sleep(0.5)
            except Exception as e:
                print(f"주식 가격 업데이트 오류: {stock.symbol} - {e}")



