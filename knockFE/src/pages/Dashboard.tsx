import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { portfolioAPI } from '../api/portfolio';
import { newsAPI } from '../api/news';
import { useAuth } from '../context/AuthContext';
import './Dashboard.css';

const Dashboard: React.FC = () => {
  const { logout, user } = useAuth();
  const navigate = useNavigate();

  const { data: portfolio } = useQuery({
    queryKey: ['portfolio'],
    queryFn: () => portfolioAPI.getAll(),
  });

  const { data: news } = useQuery({
    queryKey: ['recentNews'],
    queryFn: () => newsAPI.getRecent(7),
  });

  const totalValue = portfolio?.reduce((sum, item) => sum + item.totalValue, 0) || 0;
  const totalProfitLoss = portfolio?.reduce((sum, item) => sum + item.profitLoss, 0) || 0;

  return (
    <div className="dashboard">
      <header className="dashboard-header">
        <div className="header-left">
          <h1>StockKnock</h1>
          {user?.name && <span className="user-greeting">안녕하세요, {user.name}님!</span>}
        </div>
        <nav>
          <button onClick={() => navigate('/portfolio')}>포트폴리오</button>
          <button onClick={() => navigate('/news')}>뉴스</button>
          <button onClick={() => navigate('/ai-chat')}>AI 분석</button>
          <button onClick={() => navigate('/email-change')}>이메일 변경</button>
          <button onClick={logout}>로그아웃</button>
        </nav>
      </header>

      <main className="dashboard-content">
        <section className="portfolio-summary">
          <h2>포트폴리오 요약</h2>
          <div className="summary-cards">
            <div className="summary-card">
              <h3>총 평가액</h3>
              <p className="value">{totalValue.toLocaleString()}원</p>
            </div>
            <div className="summary-card">
              <h3>총 손익</h3>
              <p className={`value ${totalProfitLoss >= 0 ? 'positive' : 'negative'}`}>
                {totalProfitLoss >= 0 ? '+' : ''}{totalProfitLoss.toLocaleString()}원
              </p>
            </div>
          </div>
        </section>

        <section className="recent-news">
          <h2>최근 뉴스</h2>
          <div className="news-list">
            {news?.slice(0, 5).map((item) => (
              <div key={item.id} className="news-item">
                <h4>{item.title}</h4>
                <p>{item.source} · {new Date(item.published_at || '').toLocaleDateString()}</p>
              </div>
            ))}
          </div>
        </section>
      </main>
    </div>
  );
};

export default Dashboard;

