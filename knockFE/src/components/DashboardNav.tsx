import React from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import './DashboardNav.css';

interface NavItemProps {
  path: string;
  label: string;
  isActive: boolean;
  onClick: () => void;
}

const NavItem: React.FC<NavItemProps> = ({ label, isActive, onClick }) => {
  return (
    <button
      className={`nav-item ${isActive ? 'active' : ''}`}
      onClick={onClick}
    >
      {label}
    </button>
  );
};

const DashboardNav: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { user } = useAuth();

  const mainTabs = [
    { path: '/portfolio', label: '포트폴리오' },
    { path: '/watchlist', label: '관심 종목' },
    { path: '/ai-chat', label: 'AI 채팅' },
  ];

  const isActive = (path: string) => {
    return location.pathname === path || location.pathname.startsWith(path + '/');
  };

  const isDevUser = user?.username === 'dev';

  return (
    <header className="dashboard-header">
      <div className="header-left">
        <div className="logo-section">
          <h1 className="logo">StockKnock</h1>
          {isDevUser && <span className="env-badge">DEV</span>}
        </div>
      </div>
      
      <nav className="header-nav">
        {mainTabs.map((tab) => (
          <NavItem
            key={tab.path}
            path={tab.path}
            label={tab.label}
            isActive={isActive(tab.path)}
            onClick={() => navigate(tab.path)}
          />
        ))}
      </nav>

      <div className="header-right">
        <button
          className="header-action"
          onClick={() => navigate('/mypage')}
        >
          마이페이지
        </button>
      </div>
    </header>
  );
};

export default DashboardNav;
