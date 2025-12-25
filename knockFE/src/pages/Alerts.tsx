import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { alertsAPI, type CreateAlertRequest } from '../api/alerts';
import { stockAPI } from '../api/stock';
import type { StockDto } from '../api/stock';
import './Alerts.css';

const Alerts: React.FC = () => {
  const queryClient = useQueryClient();
  const [showAddModal, setShowAddModal] = useState(false);
  const [searchKeyword, setSearchKeyword] = useState('');
  const [searchResults, setSearchResults] = useState<StockDto[]>([]);
  const [selectedStock, setSelectedStock] = useState<StockDto | null>(null);
  const [alertType, setAlertType] = useState<'TARGET' | 'STOP_LOSS' | 'PERCENT'>('TARGET');
  const [targetPrice, setTargetPrice] = useState('');
  const [percentageChange, setPercentageChange] = useState('');
  const [isSearching, setIsSearching] = useState(false);

  const { data: alerts, isLoading } = useQuery({
    queryKey: ['alerts'],
    queryFn: () => alertsAPI.getAll(),
  });

  const addMutation = useMutation({
    mutationFn: (request: CreateAlertRequest) => alertsAPI.create(request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['alerts'] });
      setShowAddModal(false);
      resetForm();
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (alertId: number) => alertsAPI.delete(alertId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['alerts'] });
    },
  });

  const handleSearch = async () => {
    if (!searchKeyword.trim()) return;
    setIsSearching(true);
    try {
      const results = await stockAPI.search(searchKeyword);
      setSearchResults(results);
    } catch (error) {
      console.error('검색 실패:', error);
    } finally {
      setIsSearching(false);
    }
  };

  const handleSelectStock = (stock: StockDto) => {
    setSelectedStock(stock);
    setSearchResults([]);
    setSearchKeyword('');
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedStock) return;

    const request: CreateAlertRequest = {
      stockSymbol: selectedStock.symbol,
      alertType,
      targetPrice: alertType !== 'PERCENT' ? parseFloat(targetPrice) : undefined,
      percentageChange: alertType === 'PERCENT' ? parseFloat(percentageChange) : undefined,
    };

    addMutation.mutate(request);
  };

  const resetForm = () => {
    setSelectedStock(null);
    setSearchKeyword('');
    setSearchResults([]);
    setAlertType('TARGET');
    setTargetPrice('');
    setPercentageChange('');
  };

  const getAlertTypeLabel = (type: string) => {
    switch (type) {
      case 'TARGET':
        return '목표가';
      case 'STOP_LOSS':
        return '손절가';
      case 'PERCENT':
        return '변동률';
      default:
        return type;
    }
  };

  const formatPrice = (price: number) => {
    return price?.toLocaleString('ko-KR') || '-';
  };

  return (
    <div className="alerts-container">
      <div className="alerts-header">
        <h1>가격 알림</h1>
        <button className="btn-primary" onClick={() => setShowAddModal(true)}>
          + 알림 추가
        </button>
      </div>

      {isLoading ? (
        <div className="loading">로딩 중...</div>
      ) : alerts && alerts.length > 0 ? (
        <div className="alerts-list">
          {alerts.map((alert) => (
            <div key={alert.id} className="alert-card">
              <div className="alert-info">
                <div className="alert-stock">
                  <h3>{alert.stock.name}</h3>
                  <span className="stock-symbol">{alert.stock.symbol}</span>
                </div>
                <div className="alert-condition">
                  <span className="alert-type">{getAlertTypeLabel(alert.alertType)}</span>
                  {alert.alertType === 'PERCENT' ? (
                    <span className="alert-value">
                      {alert.percentChange && alert.percentChange > 0 ? '+' : ''}
                      {alert.percentChange}%
                    </span>
                  ) : (
                    <span className="alert-value">{formatPrice(alert.targetPrice || 0)}원</span>
                  )}
                </div>
                <div className="alert-status">
                  {alert.triggered ? (
                    <span className="status-triggered">✓ 트리거됨</span>
                  ) : (
                    <span className="status-active">활성</span>
                  )}
                </div>
              </div>
              <button
                className="btn-delete"
                onClick={() => {
                  if (window.confirm('알림을 삭제하시겠습니까?')) {
                    deleteMutation.mutate(alert.id);
                  }
                }}
                disabled={deleteMutation.isPending}
              >
                삭제
              </button>
            </div>
          ))}
        </div>
      ) : (
        <div className="empty-state">
          <p>설정된 알림이 없습니다.</p>
          <button className="btn-primary" onClick={() => setShowAddModal(true)}>
            알림 추가하기
          </button>
        </div>
      )}

      {showAddModal && (
        <div className="modal-overlay" onClick={() => setShowAddModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>알림 추가</h2>
              <button className="btn-close" onClick={() => setShowAddModal(false)}>
                ✕
              </button>
            </div>
            <form onSubmit={handleSubmit} className="alert-form">
              {!selectedStock ? (
                <div className="form-section">
                  <label>종목 검색</label>
                  <div className="search-box">
                    <input
                      type="text"
                      placeholder="종목명 또는 심볼 검색..."
                      value={searchKeyword}
                      onChange={(e) => setSearchKeyword(e.target.value)}
                      onKeyPress={(e) => e.key === 'Enter' && handleSearch()}
                    />
                    <button
                      type="button"
                      className="btn-primary"
                      onClick={handleSearch}
                      disabled={isSearching}
                    >
                      {isSearching ? '검색 중...' : '검색'}
                    </button>
                  </div>
                  {searchResults.length > 0 && (
                    <div className="search-results">
                      {searchResults.map((stock) => (
                        <div
                          key={stock.symbol}
                          className="search-result-item"
                          onClick={() => handleSelectStock(stock)}
                        >
                          <div>
                            <strong>{stock.name}</strong>
                            <span className="result-symbol">{stock.symbol}</span>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              ) : (
                <>
                  <div className="selected-stock">
                    <strong>선택된 종목: {selectedStock.name} ({selectedStock.symbol})</strong>
                    <button type="button" className="btn-link" onClick={() => setSelectedStock(null)}>
                      변경
                    </button>
                  </div>
                  <div className="form-section">
                    <label>알림 유형</label>
                    <select
                      value={alertType}
                      onChange={(e) => setAlertType(e.target.value as any)}
                      className="form-select"
                    >
                      <option value="TARGET">목표가</option>
                      <option value="STOP_LOSS">손절가</option>
                      <option value="PERCENT">변동률</option>
                    </select>
                  </div>
                  {alertType === 'PERCENT' ? (
                    <div className="form-section">
                      <label>변동률 (%)</label>
                      <input
                        type="number"
                        step="0.1"
                        placeholder="예: 5 (5% 상승 또는 하락)"
                        value={percentageChange}
                        onChange={(e) => setPercentageChange(e.target.value)}
                        required
                      />
                    </div>
                  ) : (
                    <div className="form-section">
                      <label>{alertType === 'TARGET' ? '목표가' : '손절가'} (원)</label>
                      <input
                        type="number"
                        step="0.01"
                        placeholder="가격을 입력하세요"
                        value={targetPrice}
                        onChange={(e) => setTargetPrice(e.target.value)}
                        required
                      />
                    </div>
                  )}
                  <div className="form-actions">
                    <button type="submit" className="btn-primary" disabled={addMutation.isPending}>
                      {addMutation.isPending ? '추가 중...' : '알림 추가'}
                    </button>
                    <button
                      type="button"
                      className="btn-secondary"
                      onClick={() => setShowAddModal(false)}
                    >
                      취소
                    </button>
                  </div>
                </>
              )}
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default Alerts;

