import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { portfolioAPI } from '../api/portfolio';
import './Portfolio.css';

const Portfolio: React.FC = () => {
  const queryClient = useQueryClient();
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editQuantity, setEditQuantity] = useState<string>('');
  const [editAveragePrice, setEditAveragePrice] = useState<string>('');

  const { data: portfolio, isLoading } = useQuery({
    queryKey: ['portfolio'],
    queryFn: () => portfolioAPI.getAll(),
  });

  const { data: analysis } = useQuery({
    queryKey: ['portfolioAnalysis'],
    queryFn: () => portfolioAPI.getAnalysis(),
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, quantity, avgBuyPrice }: { id: number; quantity?: number; avgBuyPrice?: number }) =>
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

  const handleEdit = (item: any) => {
    setEditingId(item.id);
    setEditQuantity(item.quantity.toString());
    setEditAveragePrice(item.avgBuyPrice.toString());
  };

  const handleSave = (id: number) => {
    updateMutation.mutate({
      id,
      quantity: parseFloat(editQuantity),
      avgBuyPrice: parseFloat(editAveragePrice),
    });
  };

  const handleDelete = (id: number) => {
    if (window.confirm('정말 삭제하시겠습니까?')) {
      deleteMutation.mutate(id);
    }
  };

  if (isLoading) return <div>로딩 중...</div>;

  return (
    <div className="portfolio">
      <h1>포트폴리오</h1>

      {analysis && (
        <div className="portfolio-analysis">
          <h2>AI 포트폴리오 분석</h2>
          <div className="analysis-summary">
            <p>총 평가액: {analysis.totalValue.toLocaleString()}원</p>
            <p className={analysis.totalProfitLoss >= 0 ? 'positive' : 'negative'}>
              총 손익: {analysis.totalProfitLoss >= 0 ? '+' : ''}{analysis.totalProfitLoss.toLocaleString()}원
              ({analysis.totalProfitLossRate >= 0 ? '+' : ''}{analysis.totalProfitLossRate.toFixed(2)}%)
            </p>
          </div>
          <div className="ai-analysis">
            <h3>AI 분석 결과</h3>
            <p>{analysis.analysis}</p>
          </div>
        </div>
      )}

      <div className="portfolio-list">
        {portfolio?.map((item) => (
          <div key={item.id} className="portfolio-item">
            <div className="stock-info">
              <h3>{item.stock.name}</h3>
              <p>{item.stock.symbol}</p>
            </div>
            {editingId === item.id ? (
              <div className="edit-form">
                <input
                  type="number"
                  value={editQuantity}
                  onChange={(e) => setEditQuantity(e.target.value)}
                  placeholder="보유량"
                />
                <input
                  type="number"
                  value={editAveragePrice}
                  onChange={(e) => setEditAveragePrice(e.target.value)}
                  placeholder="평균가"
                />
                <button onClick={() => handleSave(item.id)}>저장</button>
                <button onClick={() => setEditingId(null)}>취소</button>
              </div>
            ) : (
              <div className="portfolio-details">
                <p>보유량: {item.quantity}</p>
                <p>평균가: {item.avgBuyPrice.toLocaleString()}원</p>
                <p>현재가: {item.currentPrice.toLocaleString()}원</p>
                <p className={item.profitLoss >= 0 ? 'positive' : 'negative'}>
                  손익: {item.profitLoss >= 0 ? '+' : ''}{item.profitLoss.toLocaleString()}원
                  ({item.profitLossRate >= 0 ? '+' : ''}{item.profitLossRate.toFixed(2)}%)
                </p>
                <div className="portfolio-actions">
                  <button onClick={() => handleEdit(item)}>수정</button>
                  <button onClick={() => handleDelete(item.id)} className="delete-btn">삭제</button>
                </div>
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
};

export default Portfolio;

