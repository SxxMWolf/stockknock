import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { useAuth } from '../../context/AuthContext';
import { portfolioAPI } from '../../api/portfolio';
import { newsAPI } from '../../api/news';
import { watchlistAPI } from '../../api/watchlist';
import { alertsAPI } from '../../api/alerts';
import DashboardNav from '../../components/DashboardNav';
import PortfolioHeroCard from '../../components/PortfolioHeroCard';
import WatchlistPreview from '../../components/WatchlistPreview';
import './Dashboard.css';

const Dashboard: React.FC = () => {
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();

  // 인증되지 않은 경우 로그인 페이지로 리다이렉트
  React.useEffect(() => {
    const token = localStorage.getItem('token');
    if (!token && !isAuthenticated) {
      navigate('/login');
    }
  }, [isAuthenticated, navigate]);

  const { data: portfolio } = useQuery({
    queryKey: ['portfolio'],
    queryFn: () => portfolioAPI.getAll(),
    enabled: isAuthenticated, // 인증된 경우에만 호출
  });

  const { data: watchlist } = useQuery({
    queryKey: ['watchlist'],
    queryFn: () => watchlistAPI.getAll(),
    enabled: isAuthenticated, // 인증된 경우에만 호출
  });

  const { data: alerts } = useQuery({
    queryKey: ['alerts'],
    queryFn: () => alertsAPI.getAll(),
    enabled: isAuthenticated, // 인증된 경우에만 호출
  });

  const { data: todaySummary, isLoading: summaryLoading, error: summaryError } = useQuery({
    queryKey: ['marketBriefing'],
    queryFn: () => newsAPI.getMarketBriefing(),
    retry: 1,
    enabled: isAuthenticated, // 인증된 경우에만 호출
  });

  const totalValue = portfolio?.reduce((sum, item) => sum + item.totalValue, 0) || 0;
  const totalProfitLoss = portfolio?.reduce((sum, item) => sum + item.profitLoss, 0) || 0;
  const activeAlertsCount = alerts?.filter((alert) => !alert.triggered).length || 0;

  return (
    <div className="dashboard">
      <DashboardNav />

      <main className="dashboard-content">
        <PortfolioHeroCard
          totalValue={totalValue}
          totalProfitLoss={totalProfitLoss}
          activeAlertsCount={activeAlertsCount}
        />

        <WatchlistPreview watchlist={watchlist || []} />

        {/* 오늘의 시장 브리핑 */}
        {!summaryError && (todaySummary || summaryLoading) && (
          <section className="news-summary-section">
            <div className="section-header">
              <h2 className="section-title">오늘의 주식은</h2>
            </div>
            <div className="news-summary-content">
              {summaryLoading ? (
                <p>시장 브리핑을 생성하는 중...</p>
              ) : todaySummary ? (
                <div className="summary-text">
                  {todaySummary.split('\n').map((line, index) => (
                    <p key={index}>{line}</p>
                  ))}
                </div>
              ) : null}
            </div>
          </section>
        )}
      </main>
    </div>
  );
};

export default Dashboard;
