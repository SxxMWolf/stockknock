import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { useAuth } from '../context/AuthContext';
import { portfolioAPI } from '../api/portfolio';
import { newsAPI } from '../api/news';
import { watchlistAPI } from '../api/watchlist';
import { alertsAPI } from '../api/alerts';
import DashboardNav from '../components/DashboardNav';
import PortfolioHeroCard from '../components/PortfolioHeroCard';
import WatchlistPreview from '../components/WatchlistPreview';
import NewsSummaryCard from '../components/NewsSummaryCard';
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

  const { data: news, isLoading: newsLoading, error: newsError } = useQuery({
    queryKey: ['recentNews'],
    queryFn: () => newsAPI.getRecent(7),
    retry: 1,
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

  const { data: todaySummary, isLoading: summaryLoading } = useQuery({
    queryKey: ['todayNewsSummary'],
    queryFn: () => newsAPI.getTodaySummary(),
    retry: 1,
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

        {/* 오늘의 주요 뉴스 AI 요약 */}
        {todaySummary && (
          <section className="news-summary-section">
            <div className="section-header">
              <h2 className="section-title">오늘의 주식은</h2>
            </div>
            <div className="news-summary-content">
              {summaryLoading ? (
                <p>요약을 생성하는 중...</p>
              ) : (
                <div className="summary-text">
                  {todaySummary.split('\n').map((line, index) => (
                    <p key={index}>{line}</p>
                  ))}
                </div>
              )}
            </div>
          </section>
        )}

        <section className="news-section">
          <div className="section-header">
            <h2 className="section-title">오늘의 주요 뉴스</h2>
            <button
              className="section-link"
              onClick={() => navigate('/news')}
            >
              →
            </button>
          </div>
          <div className="news-grid">
            {newsLoading ? (
              <div className="news-empty">
                <p>뉴스를 불러오는 중...</p>
              </div>
            ) : newsError ? (
              <div className="news-empty">
                <p>뉴스를 불러오는 중 오류가 발생했습니다.</p>
                <p style={{ fontSize: '0.85rem', marginTop: '0.5rem', color: '#999' }}>
                  {newsError instanceof Error ? newsError.message : '알 수 없는 오류'}
                </p>
              </div>
            ) : news && news.length > 0 ? (
              news.slice(0, 3).map((item) => (
                <NewsSummaryCard key={item.id} news={item} />
              ))
            ) : (
              <div className="news-empty">
                <p>표시할 뉴스가 없습니다.</p>
              </div>
            )}
          </div>
        </section>
      </main>
    </div>
  );
};

export default Dashboard;
