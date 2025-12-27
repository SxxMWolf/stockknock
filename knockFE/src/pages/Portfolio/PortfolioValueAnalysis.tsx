import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { portfolioAPI } from '../../api/portfolio';
import { parseMarkdown } from '../../utils/markdownParser';
import './PortfolioValueAnalysis.css';

const PortfolioValueAnalysis: React.FC = () => {
  const navigate = useNavigate();

  const { data: portfolio, isLoading } = useQuery({
    queryKey: ['portfolio'],
    queryFn: () => portfolioAPI.getAll(),
  });

  const { data: analysis, isLoading: analysisLoading } = useQuery({
    queryKey: ['portfolioAnalysis'],
    queryFn: () => portfolioAPI.getAnalysis(),
  });

  const totalValue = portfolio?.reduce((sum, item) => sum + item.totalValue, 0) || 0;

  if (isLoading || analysisLoading) {
    return (
      <div className="portfolio-analysis-page">
        <div className="loading">로딩 중...</div>
      </div>
    );
  }

  return (
    <div className="portfolio-analysis-page">
      <button className="btn-back" onClick={() => navigate('/dashboard')}>
        ← 대시보드로
      </button>

      <div className="analysis-header">
        <h1>총 평가액 분석</h1>
        <div className="total-value-display">
          {totalValue.toLocaleString()}원
        </div>
      </div>

      {analysis && (
        <div className="analysis-content">
          <div className="analysis-card">
            <h2>포트폴리오 구성</h2>
            <div className="portfolio-breakdown">
              {portfolio && portfolio.length > 0 ? (
                portfolio.map((item) => {
                  const percentage = totalValue > 0 ? (item.totalValue / totalValue) * 100 : 0;
                  return (
                    <div key={item.id} className="breakdown-item">
                      <div className="breakdown-header">
                        <div>
                          <h3>{item.stock.name}</h3>
                          <p className="stock-symbol">{item.stock.symbol}</p>
                        </div>
                        <div className="breakdown-value">
                          {item.totalValue.toLocaleString()}원
                          <span className="percentage">({percentage.toFixed(1)}%)</span>
                        </div>
                      </div>
                      <div className="breakdown-details">
                        <div className="detail-item">
                          <span className="detail-label">보유량</span>
                          <span className="detail-value">{item.quantity}주</span>
                        </div>
                        <div className="detail-item">
                          <span className="detail-label">평균 매입가</span>
                          <span className="detail-value">{item.avgBuyPrice.toLocaleString()}원</span>
                        </div>
                        <div className="detail-item">
                          <span className="detail-label">현재가</span>
                          <span className="detail-value">{item.currentPrice.toLocaleString()}원</span>
                        </div>
                        <div className="detail-item">
                          <span className="detail-label">손익</span>
                          <span className={`detail-value ${item.profitLoss >= 0 ? 'profit' : 'loss'}`}>
                            {item.profitLoss >= 0 ? '+' : ''}{item.profitLoss.toLocaleString()}원
                            ({item.profitLossRate >= 0 ? '+' : ''}{item.profitLossRate.toFixed(2)}%)
                          </span>
                        </div>
                      </div>
                    </div>
                  );
                })
              ) : (
                <div className="empty-state">포트폴리오가 비어있습니다.</div>
              )}
            </div>
          </div>

          <div className="analysis-card">
            <h2>AI 분석</h2>
            <div className="ai-analysis-content">
              <div 
                className="analysis-text" 
                dangerouslySetInnerHTML={{ __html: parseMarkdown(analysis.analysis || '') }}
              />
              <div className="investment-style">
                <span className="style-label">투자 스타일:</span>
                <span className="style-value">{analysis.investmentStyle}</span>
              </div>
            </div>
          </div>

          <div className="analysis-summary">
            <div className="summary-item">
              <span className="summary-label">총 평가액</span>
              <span className="summary-value">{analysis.totalValue.toLocaleString()}원</span>
            </div>
            <div className="summary-item">
              <span className="summary-label">총 손익</span>
              <span className={`summary-value ${analysis.totalProfitLoss >= 0 ? 'profit' : 'loss'}`}>
                {analysis.totalProfitLoss >= 0 ? '+' : ''}{analysis.totalProfitLoss.toLocaleString()}원
              </span>
            </div>
            <div className="summary-item">
              <span className="summary-label">수익률</span>
              <span className={`summary-value ${analysis.totalProfitLossRate >= 0 ? 'profit' : 'loss'}`}>
                {analysis.totalProfitLossRate >= 0 ? '+' : ''}{analysis.totalProfitLossRate.toFixed(2)}%
              </span>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default PortfolioValueAnalysis;

