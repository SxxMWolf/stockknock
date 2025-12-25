import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { watchlistAPI } from '../api/watchlist';
import './Watchlist.css';

const Watchlist: React.FC = () => {
  const queryClient = useQueryClient();
  const [showAddModal, setShowAddModal] = useState(false);
  const [selectedSectors, setSelectedSectors] = useState<string[]>([]);
  const [showMoreStocks, setShowMoreStocks] = useState(false);

  // 모달이 열릴 때 상태 초기화
  const handleOpenModal = () => {
    setShowAddModal(true);
    setSelectedSectors([]);
    setShowMoreStocks(false);
  };

  // 종목 코드 -> 종목명 매핑 (DB에 종목명이 없을 때 사용)
  const stockNameMap: Record<string, string> = {
    '005930': '삼성전자',
    '000660': 'SK하이닉스',
    '035420': 'NAVER',
    '035720': '카카오',
    '005380': '현대자동차',
    '066570': 'LG전자',
    '068270': '셀트리온',
    '005490': 'POSCO홀딩스',
    '051910': 'LG화학',
    '006400': '삼성SDI',
    '028260': '삼성물산',
    '207940': '삼성바이오로직스',
    '000990': 'DB하이텍',
    '012330': '현대모비스',
    '373220': 'LG에너지솔루션',
    '096770': 'SK이노베이션',
    '105560': 'KB금융',
    '055550': '신한지주',
    '086790': '하나금융지주',
    '316140': '우리금융지주',
    '000270': '기아',
    '003670': '포스코홀딩스',
  };

  // 종목명 가져오기 (DB에 없으면 매핑에서 찾기)
  const getStockName = (stock: { symbol: string; name?: string }) => {
    if (stock.name && stock.name !== stock.symbol) {
      return stock.name;
    }
    return stockNameMap[stock.symbol] || stock.symbol;
  };

  // 분야별 종목 데이터
  const stocksBySector: Record<string, Array<{ symbol: string; name: string }>> = {
    '반도체': [
      { symbol: '005930', name: '삼성전자' },
      { symbol: '000660', name: 'SK하이닉스' },
      { symbol: '000990', name: 'DB하이텍' },
      { symbol: '012330', name: '현대모비스' },
    ],
    'AI': [
      { symbol: '035420', name: 'NAVER' },
      { symbol: '035720', name: '카카오' },
      { symbol: '005930', name: '삼성전자' },
      { symbol: '000660', name: 'SK하이닉스' },
    ],
    '2차전지': [
      { symbol: '051910', name: 'LG화학' },
      { symbol: '006400', name: '삼성SDI' },
      { symbol: '373220', name: 'LG에너지솔루션' },
      { symbol: '096770', name: 'SK이노베이션' },
    ],
    '금융': [
      { symbol: '105560', name: 'KB금융' },
      { symbol: '055550', name: '신한지주' },
      { symbol: '086790', name: '하나금융지주' },
      { symbol: '316140', name: '우리금융지주' },
    ],
    '자동차': [
      { symbol: '005380', name: '현대자동차' },
      { symbol: '000270', name: '기아' },
      { symbol: '012330', name: '현대모비스' },
      { symbol: '003670', name: '포스코홀딩스' },
    ],
  };

  // 인기 종목 (기본 표시)
  const popularStocks = [
    { symbol: '005930', name: '삼성전자' },
    { symbol: '000660', name: 'SK하이닉스' },
    { symbol: '035420', name: 'NAVER' },
    { symbol: '035720', name: '카카오' },
    { symbol: '005380', name: '현대자동차' },
    { symbol: '066570', name: 'LG전자' },
    { symbol: '068270', name: '셀트리온' },
    { symbol: '005490', name: 'POSCO홀딩스' },
    { symbol: '051910', name: 'LG화학' },
    { symbol: '006400', name: '삼성SDI' },
    { symbol: '028260', name: '삼성물산' },
    { symbol: '207940', name: '삼성바이오로직스' },
  ];

  // 분야 필터에 따른 종목 필터링
  const getFilteredStocks = () => {
    if (selectedSectors.length === 0) {
      return popularStocks;
    }
    
    const filtered: Array<{ symbol: string; name: string }> = [];
    const seen = new Set<string>();
    
    selectedSectors.forEach(sector => {
      stocksBySector[sector]?.forEach(stock => {
        if (!seen.has(stock.symbol)) {
          seen.add(stock.symbol);
          filtered.push(stock);
        }
      });
    });
    
    return filtered;
  };

  const filteredStocks = getFilteredStocks();
  const displayedStocks = showMoreStocks ? filteredStocks : filteredStocks.slice(0, 6);

  const sectors = ['반도체', 'AI', '2차전지', '금융', '자동차'];

  const toggleSector = (sector: string) => {
    setSelectedSectors(prev => 
      prev.includes(sector) 
        ? prev.filter(s => s !== sector)
        : [...prev, sector]
    );
    setShowMoreStocks(false);
  };

  const { data: watchlist, isLoading } = useQuery({
    queryKey: ['watchlist'],
    queryFn: () => watchlistAPI.getAll(),
  });

  const addMutation = useMutation({
    mutationFn: (stockSymbol: string) => watchlistAPI.add(stockSymbol),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['watchlist'] });
    },
    onError: (error: any) => {
      // 에러가 발생해도 로그아웃되지 않도록 에러만 표시
      const errorMessage = error.response?.data?.error || error.response?.data?.message || '종목 추가에 실패했습니다.';
      console.error('종목 추가 오류:', errorMessage);
      // 필요시 사용자에게 알림 표시 가능
    },
  });

  const removeMutation = useMutation({
    mutationFn: (stockSymbol: string) => watchlistAPI.remove(stockSymbol),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['watchlist'] });
    },
  });

  const handleAddStock = (stockSymbol: string) => {
    addMutation.mutate(stockSymbol);
  };

  const handleRemoveStock = (stockSymbol: string) => {
    if (window.confirm('관심 종목에서 제거하시겠습니까?')) {
      removeMutation.mutate(stockSymbol);
    }
  };

  const formatPrice = (price: number) => {
    if (!price || price === 0) return '-';
    return price.toLocaleString('ko-KR') + '원';
  };

  const formatChange = (current: number, previous: number) => {
    if (!current || !previous) return { value: '-', isPositive: true };
    const change = current - previous;
    const percent = ((change / previous) * 100).toFixed(2);
    return {
      value: `${change >= 0 ? '+' : ''}${change.toLocaleString()} (${percent}%)`,
      isPositive: change >= 0,
    };
  };

  // 업데이트 시점 포맷팅
  const formatLastUpdated = (lastUpdated?: string) => {
    if (!lastUpdated) return '업데이트 정보 없음';
    
    try {
      const updated = new Date(lastUpdated);
      const now = new Date();
      const diffMs = now.getTime() - updated.getTime();
      const diffMins = Math.floor(diffMs / 60000);
      
      if (diffMins < 1) return '방금 전';
      if (diffMins < 60) return `${diffMins}분 전`;
      
      const diffHours = Math.floor(diffMins / 60);
      if (diffHours < 24) return `${diffHours}시간 전`;
      
      const diffDays = Math.floor(diffHours / 24);
      return `${diffDays}일 전`;
    } catch {
      return '업데이트 정보 없음';
    }
  };

  return (
    <div className="watchlist-container">
      <div className="watchlist-header">
        <h1 className="header-title">관심 종목</h1>
        <button className="btn-primary btn-add-stock" onClick={handleOpenModal}>
          + 종목 추가
        </button>
      </div>

      {isLoading ? (
        <div className="loading">로딩 중...</div>
      ) : watchlist && watchlist.length > 0 ? (
        <div className="watchlist-list">
          {watchlist.map((stock) => {
            const change = formatChange(stock.currentPrice || 0, stock.previousClose || 0);
            const stockName = getStockName(stock);
            return (
              <div key={stock.symbol} className="watchlist-item">
                <div className="item-left">
                  <div className="item-name">{stockName}</div>
                  <div className="item-symbol">{stock.symbol}</div>
                </div>
                <div className="item-right">
                  <div className="item-price-info">
                    <div className="item-price">
                      <span className="price-main">{formatPrice(stock.currentPrice)}</span>
                      {stock.currentPrice && stock.previousClose ? (
                        <span className={`price-change ${change.isPositive ? 'up' : 'down'}`}>
                          {change.isPositive ? '▲' : '▼'} {change.value}
                        </span>
                      ) : (
                        <span className="price-change">가격 정보 없음</span>
                      )}
                    </div>
                    <div className="item-update-info">
                      최근 업데이트: {formatLastUpdated(stock.lastUpdated)}
                    </div>
                    <div className="item-update-hint">
                      약 10분 주기로 갱신
                    </div>
                  </div>
                  <button
                    className="btn-remove"
                    onClick={() => handleRemoveStock(stock.symbol)}
                    disabled={removeMutation.isPending}
                    type="button"
                  >
                    ✕
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      ) : (
        <div className="empty-state">
          <p>관심 종목이 없습니다.</p>
          <p className="empty-hint">위의 "종목 추가" 버튼을 클릭하여 관심 종목을 추가하세요.</p>
        </div>
      )}

      {showAddModal && (
        <div className="modal-overlay" onClick={() => setShowAddModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>종목 추가</h2>
              <button className="btn-close" onClick={() => setShowAddModal(false)}>
                ✕
              </button>
            </div>
            <div className="modal-body">
              <div className="example-stocks-section">
                <h3 className="example-title">🔥 인기 종목</h3>
                <p className="example-description">아래 종목 중 하나를 선택하여 관심 종목에 추가하세요.</p>
                <div className="example-stocks-grid">
                  {displayedStocks.map((stock) => {
                    const isAdded = watchlist?.some((w) => w.symbol === stock.symbol);
                    return (
                      <button
                        key={stock.symbol}
                        className={`example-stock-item ${isAdded ? 'added' : ''}`}
                        onClick={() => !isAdded && handleAddStock(stock.symbol)}
                        disabled={addMutation.isPending || isAdded}
                        type="button"
                      >
                        <div className="example-stock-info">
                          <span className="example-stock-name">{stock.name}</span>
                          <span className="example-stock-symbol">{stock.symbol}</span>
                        </div>
                        {isAdded ? (
                          <span className="example-stock-status">✓ 추가됨</span>
                        ) : (
                          <span className="example-stock-add">+ 추가</span>
                        )}
                      </button>
                    );
                  })}
                </div>
                {filteredStocks.length > 6 && !showMoreStocks && (
                  <button
                    className="btn-show-more"
                    onClick={() => setShowMoreStocks(true)}
                    type="button"
                  >
                    + 다른 종목 찾아보기 ({filteredStocks.length - 6}개 더)
                  </button>
                )}
              </div>

              <div className="sector-filter-section">
                <h3 className="example-title">🧭 분야별 보기</h3>
                <div className="sector-filter-buttons">
                  {sectors.map((sector) => (
                    <button
                      key={sector}
                      className={`sector-filter-btn ${selectedSectors.includes(sector) ? 'active' : ''}`}
                      onClick={() => toggleSector(sector)}
                      type="button"
                    >
                      {sector}
                    </button>
                  ))}
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Watchlist;

