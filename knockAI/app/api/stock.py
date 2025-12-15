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
    price = await service.get_current_price(symbol)
    
    if not price:
        raise HTTPException(status_code=404, detail="주가를 찾을 수 없습니다.")
    
    # 최신 히스토리에서 상세 정보 가져오기
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

