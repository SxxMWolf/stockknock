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

    def _convert_korean_symbol(self, symbol: str) -> str:
        """한국 주식 심볼을 Yahoo Finance 형식으로 변환 (예: 005930 -> 005930.KS)"""
        # 6자리 숫자로 시작하는 한국 주식 심볼인 경우
        if symbol.isdigit() and len(symbol) == 6:
            # 코스피/코스닥 구분 없이 일단 .KS로 시도 (대부분 코스피)
            return f"{symbol}.KS"
        return symbol

    async def get_current_price(self, symbol: str) -> Optional[Decimal]:
        """현재 주가 조회 (Yahoo Finance 우선)"""
        try:
            print(f"가격 조회 시작: {symbol}")
            if self.yahoo_finance_enabled:
                # 한국 주식 심볼 변환
                yahoo_symbol = self._convert_korean_symbol(symbol)
                print(f"한국 주식 심볼 변환: {symbol} -> {yahoo_symbol}")
                price = await self._get_price_from_yahoo_finance(yahoo_symbol)
                if price:
                    print(f"가격 조회 성공 (Yahoo Finance .KS): {symbol} = {price}")
                    return price
                # .KS로 실패하면 .KQ (코스닥)로 재시도
                if symbol.isdigit() and len(symbol) == 6:
                    yahoo_symbol_kq = f"{symbol}.KQ"
                    print(f"코스닥 심볼로 재시도: {yahoo_symbol_kq}")
                    price = await self._get_price_from_yahoo_finance(yahoo_symbol_kq)
                    if price:
                        print(f"가격 조회 성공 (Yahoo Finance .KQ): {symbol} = {price}")
                        return price

            if self.alpha_vantage_key:
                print(f"Alpha Vantage 조회 시도: {symbol}")
                price = await self._get_price_from_alpha_vantage(symbol)
                if price:
                    print(f"가격 조회 성공 (Alpha Vantage): {symbol} = {price}")
                    return price

            if self.twelve_data_key:
                print(f"Twelve Data 조회 시도: {symbol}")
                price = await self._get_price_from_twelve_data(symbol)
                if price:
                    print(f"가격 조회 성공 (Twelve Data): {symbol} = {price}")
                    return price

            print(f"모든 API에서 가격 조회 실패: {symbol}")
            return None
        except Exception as e:
            print(f"주가 조회 오류: {symbol} - {e}")
            import traceback
            traceback.print_exc()
            return None

    async def _get_price_from_yahoo_finance(self, symbol: str) -> Optional[Decimal]:
        """Yahoo Finance API를 통한 주가 조회"""
        try:
            print(f"Yahoo Finance 조회 시작: {symbol}")
            import asyncio
            
            # yfinance는 동기 라이브러리이므로 executor에서 실행
            def get_price_sync():
                ticker = yf.Ticker(symbol)
                # info() 메서드로 현재가 조회 (더 빠르고 안정적)
                try:
                    info = ticker.info
                    if info and isinstance(info, dict):
                        if 'currentPrice' in info and info['currentPrice']:
                            return Decimal(str(info['currentPrice']))
                        elif 'regularMarketPrice' in info and info['regularMarketPrice']:
                            return Decimal(str(info['regularMarketPrice']))
                except Exception as info_error:
                    print(f"info() 메서드 실패: {symbol} - {info_error}")
                
                # history() 메서드로 시도
                try:
                    data = ticker.history(period="1d")
                    if not data.empty:
                        latest = data.iloc[-1]
                        return Decimal(str(latest['Close']))
                except Exception as hist_error:
                    print(f"history() 메서드 실패: {symbol} - {hist_error}")
                
                return None
            
            # 비동기로 실행
            loop = asyncio.get_event_loop()
            price = await loop.run_in_executor(None, get_price_sync)
            
            if price:
                print(f"Yahoo Finance 가격 조회 성공: {symbol} = {price}")
                return price
            
            print(f"Yahoo Finance 가격 조회 실패: {symbol}")
            return None
        except Exception as e:
            print(f"Yahoo Finance API 오류: {symbol} - {e}")
            import traceback
            traceback.print_exc()
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

            # Yahoo Finance에서 상세 정보 가져오기 (한국 주식 심볼 변환)
            yahoo_symbol = self._convert_korean_symbol(symbol)
            async with httpx.AsyncClient(timeout=10.0) as client:
                url = f"https://query1.finance.yahoo.com/v8/finance/chart/{yahoo_symbol}?interval=1d&range=1d"
                response = await client.get(url)
                
                # .KS로 실패하면 .KQ로 재시도
                if response.status_code != 200 and symbol.isdigit() and len(symbol) == 6:
                    yahoo_symbol_kq = f"{symbol}.KQ"
                    url = f"https://query1.finance.yahoo.com/v8/finance/chart/{yahoo_symbol_kq}?interval=1d&range=1d"
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



