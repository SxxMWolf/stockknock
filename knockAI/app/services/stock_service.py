import asyncio
import httpx
from decimal import Decimal
from datetime import datetime, timedelta
from typing import Optional, Dict
from sqlalchemy.orm import Session
from sqlalchemy import select
from app.models import Stock, StockPriceHistory
import os
from dotenv import load_dotenv
import yfinance as yf
import pytz

load_dotenv()


class StockService:
    # 실패 캐시: {symbol: last_failed_at}
    _failure_cache: Dict[str, datetime] = {}
    _cache_ttl_minutes = 5  # 5분간 캐시 유지
    
    def _is_market_hours(self) -> bool:
        """한국 주식 시장 장중 시간 체크 (09:00 ~ 15:30 KST)"""
        kst = pytz.timezone('Asia/Seoul')
        now = datetime.now(kst)
        current_time = now.time()
        # 장중: 09:00 ~ 15:30
        return current_time >= datetime.strptime("09:00", "%H:%M").time() and \
               current_time <= datetime.strptime("15:30", "%H:%M").time()
    
    def __init__(self, db: Session):
        self.db = db
        self.alpha_vantage_key = os.getenv("ALPHA_VANTAGE_API_KEY", "")
        self.twelve_data_key = os.getenv("TWELVE_DATA_API_KEY", "")
        self.yahoo_finance_enabled = os.getenv("YAHOO_FINANCE_ENABLED", "true").lower() == "true"
    
    def _is_failed_recently(self, symbol: str) -> bool:
        """최근 5분 이내에 실패한 적이 있는지 확인"""
        if symbol not in self._failure_cache:
            return False
        
        last_failed_at = self._failure_cache[symbol]
        now = datetime.now()
        time_diff = now - last_failed_at
        
        # 5분 이내면 True 반환 (재시도 금지)
        if time_diff < timedelta(minutes=self._cache_ttl_minutes):
            return True
        
        # 5분이 지났으면 캐시에서 제거
        del self._failure_cache[symbol]
        return False
    
    def _record_failure(self, symbol: str):
        """실패 기록을 캐시에 저장"""
        self._failure_cache[symbol] = datetime.now()

    def _convert_korean_symbol(self, symbol: str) -> str:
        """한국 주식 심볼을 Yahoo Finance 형식으로 변환 (예: 005930 -> 005930.KS)"""
        # 6자리 숫자로 시작하는 한국 주식 심볼인 경우
        if symbol.isdigit() and len(symbol) == 6:
            # 코스피/코스닥 구분 없이 일단 .KS로 시도 (대부분 코스피)
            return f"{symbol}.KS"
        return symbol

    async def get_current_price(self, symbol: str) -> Optional[Decimal]:
        """현재 주가 조회 (장중에는 외부 API 호출 금지, 캐시만 사용)"""
        try:
            # 장중 시간 체크: 장중일 때는 외부 API 호출 금지
            if self._is_market_hours():
                print(f"[PRICE] Market hours: external API call disabled for {symbol}")
                return None
            
            # 실패 캐시 확인: 최근 5분 이내 실패한 경우 바로 None 반환
            if self._is_failed_recently(symbol):
                print(f"[PRICE] External API call skipped (recent failure): {symbol}")
                return None

            # Alpha Vantage 시도 (한 번만, 실패 시 즉시 다음으로)
            if self.alpha_vantage_key:
                price = await self._get_price_from_alpha_vantage(symbol)
                if price:
                    return price

            # Twelve Data 시도 (한 번만, 실패 시 즉시 종료)
            if self.twelve_data_key:
                price = await self._get_price_from_twelve_data(symbol)
                if price:
                    return price

            # 모든 API 실패
            print(f"[PRICE] External API failed: {symbol}")
            # 실패 기록 저장
            self._record_failure(symbol)
            print(f"[PRICE] Final result: null")
            return None
        except Exception as e:
            # 예외 발생 시에도 실패 기록 저장
            self._record_failure(symbol)
            print(f"[PRICE] External API failed: {symbol}")
            print(f"[PRICE] Final result: null")
            return None

    async def _get_price_from_yahoo_finance(self, symbol: str) -> Optional[Decimal]:
        """Yahoo Finance API를 통한 주가 조회 (비활성화됨)"""
        # Yahoo Finance는 현재 비활성화됨 (429 Too Many Requests 방지)
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
        except Exception:
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
        except Exception:
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

            # Yahoo Finance 임시 비활성화 (429 Too Many Requests 방지)
            # 상세 정보 없이 기본 가격만 저장
            # yahoo_symbol = self._convert_korean_symbol(symbol)
            # async with httpx.AsyncClient(timeout=10.0) as client:
            #     url = f"https://query1.finance.yahoo.com/v8/finance/chart/{yahoo_symbol}?interval=1d&range=1d"
            #     response = await client.get(url)
            #     
            #     # .KS로 실패하면 .KQ로 재시도
            #     if response.status_code != 200 and symbol.isdigit() and len(symbol) == 6:
            #         yahoo_symbol_kq = f"{symbol}.KQ"
            #         url = f"https://query1.finance.yahoo.com/v8/finance/chart/{yahoo_symbol_kq}?interval=1d&range=1d"
            #         response = await client.get(url)
            #     
            #     if response.status_code == 200:
            #         data = response.json()
            #         if "chart" in data and "result" in data["chart"]:
            #             result = data["chart"]["result"]
            #             if result and len(result) > 0:
            #                 meta = result[0].get("meta", {})
            #                 
            #                 # StockPriceHistory 저장
            #                 price_history = StockPriceHistory(
            #                     stock_symbol=symbol,
            #                     price=price,
            #                     open=Decimal(str(meta.get("previousClose", 0))) if meta.get("previousClose") else None,
            #                     high=Decimal(str(meta.get("regularMarketDayHigh", 0))) if meta.get("regularMarketDayHigh") else None,
            #                     low=Decimal(str(meta.get("regularMarketDayLow", 0))) if meta.get("regularMarketDayLow") else None,
            #                     volume=int(meta.get("regularMarketVolume", 0)) if meta.get("regularMarketVolume") else None,
            #                     timestamp=datetime.now()
            #                 )
            #                 
            #                 self.db.add(price_history)
            #                 self.db.commit()
            #                 return True
            
            # 기본 가격만 저장 (Yahoo Finance 비활성화로 인해 상세 정보 없음)
            price_history = StockPriceHistory(
                stock_symbol=symbol,
                price=price,
                open=None,
                high=None,
                low=None,
                volume=None,
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



