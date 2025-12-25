import React from 'react';
import { useNavigate } from 'react-router-dom';
import './PortfolioHeroCard.css';

interface PortfolioHeroCardProps {
  totalValue: number;
  totalProfitLoss: number;
  activeAlertsCount: number;
}

const PortfolioHeroCard: React.FC<PortfolioHeroCardProps> = ({
  totalValue,
  totalProfitLoss,
  activeAlertsCount,
}) => {
  const navigate = useNavigate();
  const isProfit = totalProfitLoss >= 0;

  return (
    <div className="portfolio-hero">
      <div 
        className="hero-main-card clickable"
        onClick={() => navigate('/portfolio/analysis/value')}
      >
        <div className="hero-label">총 평가액</div>
        <div className="hero-value">{totalValue.toLocaleString()}원</div>
      </div>
      <div className="hero-secondary-cards">
        <div 
          className="hero-secondary-card clickable"
          onClick={() => navigate('/portfolio/analysis/profit')}
        >
          <div className="secondary-label">오늘 손익</div>
          <div className={`secondary-value ${isProfit ? 'profit' : 'loss'}`}>
            {isProfit ? '+' : ''}{totalProfitLoss.toLocaleString()}원
          </div>
        </div>
        <div className="hero-secondary-card">
          <div className="secondary-label">활성 알림</div>
          <div className="secondary-value neutral">{activeAlertsCount}개</div>
        </div>
      </div>
    </div>
  );
};

export default PortfolioHeroCard;
