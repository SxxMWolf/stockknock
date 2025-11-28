import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { portfolioAPI } from '../api/portfolio';
import './Portfolio.css';

const Portfolio: React.FC = () => {
  const { data: portfolio, isLoading } = useQuery({
    queryKey: ['portfolio'],
    queryFn: () => portfolioAPI.getAll(),
  });

  if (isLoading) return <div>로딩 중...</div>;

  return (
    <div className="portfolio">
      <h1>포트폴리오</h1>
      <div className="portfolio-list">
        {portfolio?.map((item) => (
          <div key={item.id} className="portfolio-item">
            <div className="stock-info">
              <h3>{item.stock.name}</h3>
              <p>{item.stock.symbol}</p>
            </div>
            <div className="portfolio-details">
              <p>보유량: {item.quantity}</p>
              <p>평균가: {item.averagePrice.toLocaleString()}원</p>
              <p>현재가: {item.currentPrice.toLocaleString()}원</p>
              <p className={item.profitLoss >= 0 ? 'positive' : 'negative'}>
                손익: {item.profitLoss >= 0 ? '+' : ''}{item.profitLoss.toLocaleString()}원
                ({item.profitLossRate >= 0 ? '+' : ''}{item.profitLossRate.toFixed(2)}%)
              </p>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default Portfolio;

