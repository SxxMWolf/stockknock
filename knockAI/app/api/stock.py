from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from decimal import Decimal
from datetime import datetime
from app.database import get_db
from app.services.stock_service import StockService
from app.schemas import StockPriceResponse, StockPriceUpdateRequest
from typing import Optional

router = APIRouter(prefix="/api/stock", tags=["stock"])


@router.get("/{symbol}/price", response_model=StockPriceResponse)
async def get_current_price(
    symbol: str,
    db: Session = Depends(get_db)
):
    """현재 주가 조회"""
    service = StockService(db)
    
    # 최신 히스토리에서 상세 정보 가져오기 (DB 우선)
    from sqlalchemy import select, desc
    from app.models import StockPriceHistory
    
    latest_history = db.execute(
        select(StockPriceHistory)
        .where(StockPriceHistory.stock_symbol == symbol)
        .order_by(desc(StockPriceHistory.timestamp))
        .limit(1)
    ).scalar_one_or_none()
    
    if latest_history:
        return StockPriceResponse(
            symbol=symbol,
            price=latest_history.price,
            open=latest_history.open,
            high=latest_history.high,
            low=latest_history.low,
            volume=int(latest_history.volume) if latest_history.volume else None,
            timestamp=latest_history.timestamp
        )
    
    # DB에 없으면 FastAPI에서 가격 조회 시도
    print(f"FastAPI: DB에 가격 이력 없음, 외부 API에서 조회 시도: {symbol}")
    price = await service.get_current_price(symbol)
    
    if not price:
        print(f"FastAPI: 가격 조회 실패: {symbol}")
        # 404 대신 0을 반환하여 Spring Boot에서 처리하도록 함
        return StockPriceResponse(
            symbol=symbol,
            price=Decimal("0"),
            timestamp=datetime.now()
        )
    
    print(f"FastAPI: 가격 조회 성공: {symbol} = {price}")
    return StockPriceResponse(
        symbol=symbol,
        price=price,
        timestamp=datetime.now()
    )


@router.post("/{symbol}/update")
async def update_stock_price(
    symbol: str,
    db: Session = Depends(get_db)
):
    """주가 업데이트"""
    service = StockService(db)
    success = await service.update_stock_price(symbol)
    
    if not success:
        raise HTTPException(status_code=500, detail="주가 업데이트 실패")
    
    return {"message": "주가가 업데이트되었습니다.", "symbol": symbol}


@router.post("/update-all")
async def update_all_stock_prices(
    db: Session = Depends(get_db)
):
    """모든 주식 가격 업데이트"""
    service = StockService(db)
    await service.update_all_stock_prices()
    
    return {"message": "모든 주가가 업데이트되었습니다."}

