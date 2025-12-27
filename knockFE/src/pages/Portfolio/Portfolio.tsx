import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { portfolioAPI } from '../../api/portfolio';
import { alertsAPI } from '../../api/alerts';
import { parseMarkdown } from '../../utils/markdownParser';
import PortfolioHeroCard from '../../components/PortfolioHeroCard';
import './Portfolio.css';

type StockOption = {
  symbol: string;
  name: string;
};

const STOCK_OPTIONS: StockOption[] = [
  { symbol: '005930', name: '삼성전자' },
  { symbol: '000660', name: 'SK하이닉스' },
  { symbol: '035420', name: 'NAVER' },
  { symbol: '035720', name: '카카오' },
  { symbol: '005380', name: '현대자동차' },
  { symbol: '066570', name: 'LG전자' },
  { symbol: '068270', name: '셀트리온' },
  { symbol: '005490', name: 'POSCO홀딩스' },
];

const Portfolio: React.FC = () => {
  const queryClient = useQueryClient();

  const [editingId, setEditingId] = useState<number | null>(null);
  const [editQuantity, setEditQuantity] = useState('');
  const [editAveragePrice, setEditAveragePrice] = useState('');

  const [showAddModal, setShowAddModal] = useState(false);
  const [selectedStock, setSelectedStock] = useState<StockOption | null>(null);
  const [addQuantity, setAddQuantity] = useState('');
  const [addAvgPrice, setAddAvgPrice] = useState('');

  const { data: portfolio, isLoading } = useQuery({
    queryKey: ['portfolio'],
    queryFn: () => portfolioAPI.getAll(),
    onSuccess: (data) => {
      console.log('Portfolio data:', data);
      if (data && data.length > 0) {
        console.log('First item stock:', data[0].stock);
      }
    },
  });

  const { data: analysis, error: analysisError } = useQuery({
    queryKey: ['portfolioAnalysis'],
    queryFn: () => portfolioAPI.getAnalysis(),
    retry: 1,
  });

  const { data: alerts } = useQuery({
    queryKey: ['alerts'],
    queryFn: () => alertsAPI.getAll(),
  });

  const totalValue = portfolio?.reduce((sum, item) => sum + item.totalValue, 0) || 0;
  const totalProfitLoss = portfolio?.reduce((sum, item) => sum + item.profitLoss, 0) || 0;
  const activeAlertsCount = alerts?.filter((alert) => !alert.triggered).length || 0;

  const updateMutation = useMutation({
    mutationFn: ({ id, quantity, avgBuyPrice }: { id: number; quantity: number; avgBuyPrice: number }) =>
      portfolioAPI.update(id, quantity, avgBuyPrice),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['portfolio'] });
      queryClient.invalidateQueries({ queryKey: ['portfolioAnalysis'] });
      setEditingId(null);
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => portfolioAPI.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['portfolio'] });
      queryClient.invalidateQueries({ queryKey: ['portfolioAnalysis'] });
    },
  });

  const addMutation = useMutation({
    mutationFn: ({ symbol, quantity, avgPrice }: { symbol: string; quantity: number; avgPrice: number }) =>
      portfolioAPI.add(symbol, quantity, avgPrice), // avgPrice를 avgBuyPrice로 전달
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['portfolio'] });
      queryClient.invalidateQueries({ queryKey: ['portfolioAnalysis'] });
      setShowAddModal(false);
      setSelectedStock(null);
      setAddQuantity('');
      setAddAvgPrice('');
    },
  });

  const handleAdd = () => {
    if (!selectedStock || !addQuantity || !addAvgPrice) {
      alert('모든 필드를 입력해주세요.');
      return;
    }

    const quantityNum = Number(addQuantity);
    const avgPriceNum = Number(addAvgPrice);
    
    console.log('Adding portfolio item:', {
      symbol: selectedStock.symbol,
      quantity: quantityNum,
      avgPrice: avgPriceNum
    });

    if (isNaN(quantityNum) || isNaN(avgPriceNum)) {
      alert('숫자를 올바르게 입력해주세요.');
      return;
    }

    addMutation.mutate({
      symbol: selectedStock.symbol,
      quantity: quantityNum,
      avgPrice: avgPriceNum,
    });
  };

  const handleEdit = (item: any) => {
    setEditingId(item.id);
    setEditQuantity(item.quantity.toString());
    setEditAveragePrice(item.avgBuyPrice.toString());
  };

  const handleSave = (id: number) => {
    updateMutation.mutate({
      id,
      quantity: Number(editQuantity),
      avgBuyPrice: Number(editAveragePrice),
    });
  };

  const handleDelete = (id: number) => {
    if (window.confirm('정말 삭제하시겠습니까?')) {
      deleteMutation.mutate(id);
    }
  };

  if (isLoading) return <div className="portfolio-loading">로딩 중...</div>;

  return (
    <div className="portfolio">
      <div className="portfolio-header">
        <h1>포트폴리오</h1>
      </div>

      {/* 포트폴리오 요약 카드 */}
      {portfolio && portfolio.length > 0 && (
        <PortfolioHeroCard
          totalValue={totalValue}
          totalProfitLoss={totalProfitLoss}
          activeAlertsCount={activeAlertsCount}
        />
      )}

      {/* 종목 추가 모달 */}
      {showAddModal && (
        <div className="modal-overlay" onClick={() => setShowAddModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <h2>종목 추가</h2>

            <div className="form-group">
              <label>종목</label>
              <select
                value={selectedStock?.symbol ?? ''}
                onChange={(e) => {
                  const stock = STOCK_OPTIONS.find(s => s.symbol === e.target.value);
                  setSelectedStock(stock ?? null);
                }}
              >
                <option value="">종목을 선택하세요</option>
                {STOCK_OPTIONS.map(stock => (
                  <option key={stock.symbol} value={stock.symbol}>
                    {stock.name} ({stock.symbol})
                  </option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label>보유량</label>
              <input
                type="number"
                value={addQuantity}
                onChange={(e) => setAddQuantity(e.target.value)}
                placeholder="예: 10"
              />
            </div>

            <div className="form-group">
              <label>평균 매수가</label>
              <input
                type="number"
                value={addAvgPrice}
                onChange={(e) => setAddAvgPrice(e.target.value)}
                placeholder="예: 75000"
              />
            </div>

            <div className="modal-actions">
              <button onClick={handleAdd} disabled={addMutation.isPending}>
                {addMutation.isPending ? '추가 중...' : '추가'}
              </button>
              <button onClick={() => setShowAddModal(false)}>취소</button>
            </div>
          </div>
        </div>
      )}

      {/* 포트폴리오 리스트 */}
      {portfolio && portfolio.length > 0 && (
        <div className="portfolio-section">
          <div className="section-header">
            <h2 className="section-title">보유 종목</h2>
            <button className="add-stock-btn" onClick={() => setShowAddModal(true)} title="종목 추가">
              +
            </button>
          </div>
          <div className="portfolio-list">
            {portfolio.map((item: any) => (
              <div key={item.id} className="portfolio-item">
                <div className="stock-info">
                  <h3>{item.stock?.name || '종목명 없음'}</h3>
                  <p className="stock-symbol">{item.stock?.symbol || ''}</p>
                </div>

                {editingId === item.id ? (
                  <div className="edit-form">
                    <div className="edit-input-group">
                      <label>보유량</label>
                      <input
                        type="number"
                        value={editQuantity}
                        onChange={(e) => setEditQuantity(e.target.value)}
                        placeholder="보유량"
                      />
                    </div>
                    <div className="edit-input-group">
                      <label>평균가</label>
                      <input
                        type="number"
                        value={editAveragePrice}
                        onChange={(e) => setEditAveragePrice(e.target.value)}
                        placeholder="평균가"
                      />
                    </div>
                    <div className="edit-actions">
                      <button className="save-btn" onClick={() => handleSave(item.id)}>
                        저장
                      </button>
                      <button className="cancel-btn" onClick={() => setEditingId(null)}>
                        취소
                      </button>
                    </div>
                  </div>
                ) : (
                  <div className="portfolio-details">
                    <div className="detail-row">
                      <span className="detail-label">보유량</span>
                      <span className="detail-value">{item.quantity}주</span>
                    </div>
                    <div className="detail-row">
                      <span className="detail-label">평균가</span>
                      <span className="detail-value">{item.avgBuyPrice.toLocaleString()}원</span>
                    </div>
                    <div className="detail-row">
                      <span className="detail-label">현재가</span>
                      <span className="detail-value">{item.currentPrice.toLocaleString()}원</span>
                    </div>
                    <div className="detail-row">
                      <span className="detail-label">평가금액</span>
                      <span className="detail-value">{item.totalValue.toLocaleString()}원</span>
                    </div>
                    <div className={`detail-row profit-loss ${item.profitLoss >= 0 ? 'positive' : 'negative'}`}>
                      <span className="detail-label">손익</span>
                      <span className="detail-value">
                        {item.profitLoss >= 0 ? '+' : ''}
                        {item.profitLoss.toLocaleString()}원
                        <span className="profit-loss-rate">({item.profitLossRate >= 0 ? '+' : ''}{item.profitLossRate.toFixed(2)}%)</span>
                      </span>
                    </div>
                    <div className="portfolio-actions">
                      <button onClick={() => handleEdit(item)}>수정</button>
                      <button className="delete-btn" onClick={() => handleDelete(item.id)}>
                        삭제
                      </button>
                    </div>
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      {/* AI 분석 */}
      <div className="portfolio-section">
        <h2 className="section-title">AI 포트폴리오 분석</h2>
        <div className="portfolio-analysis">
          {(!portfolio || portfolio.length === 0) ? (
            <div className="empty-portfolio-analysis">
              <div className="empty-icon">📊</div>
              <p>포트폴리오가 비어있습니다.</p>
              <p className="empty-subtitle">첫 번째 종목을 추가해보세요.</p>
              <button className="btn-primary" onClick={() => setShowAddModal(true)}>
                종목 추가하기
              </button>
            </div>
          ) : analysisError ? (
            <div className="analysis-error">
              <p>포트폴리오 분석 중 오류가 발생했습니다.</p>
              <p className="error-detail">FastAPI 서버를 확인해주세요.</p>
            </div>
          ) : analysis ? (
            <div
              className="analysis-content"
              dangerouslySetInnerHTML={{
                __html: parseMarkdown(analysis.analysis || '포트폴리오 분석 결과가 없습니다.'),
              }}
            />
          ) : (
            <div className="analysis-loading">
              <p>분석 중...</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default Portfolio;

