import React from 'react';
import { useNavigate } from 'react-router-dom';
import type { NewsDto } from '../api/news';
import './NewsSummaryCard.css';

interface NewsSummaryCardProps {
  news: NewsDto;
}

const NewsSummaryCard: React.FC<NewsSummaryCardProps> = ({ news }) => {
  const navigate = useNavigate();

  const getSentimentColor = (sentiment: string) => {
    switch (sentiment?.toUpperCase()) {
      case 'POSITIVE':
        return '#16a34a';
      case 'NEGATIVE':
        return '#111827';
      case 'NEUTRAL':
        return '#666';
      default:
        return '#666';
    }
  };

  const getSentimentLabel = (sentiment: string) => {
    switch (sentiment?.toUpperCase()) {
      case 'POSITIVE':
        return '긍정';
      case 'NEGATIVE':
        return '부정';
      case 'NEUTRAL':
        return '중립';
      default:
        return '분석 중';
    }
  };

  return (
    <div
      className="news-summary-card"
      onClick={() => navigate(`/news/${news.id}`)}
    >
      <div className="news-card-header">
        <h3 className="news-title">{news.title}</h3>
        {news.analysis && (
          <span
            className="ai-badge"
            style={{ backgroundColor: getSentimentColor(news.analysis.sentiment) }}
          >
            AI {getSentimentLabel(news.analysis.sentiment)}
          </span>
        )}
      </div>
      
      <div className="news-card-meta">
        <span className="news-source">{news.source}</span>
        <span className="news-date">
          {new Date(news.publishedAt || '').toLocaleDateString('ko-KR', {
            month: 'short',
            day: 'numeric',
          })}
        </span>
      </div>

      {news.analysis?.summary && (
        <p className="news-summary-text">{news.analysis.summary}</p>
      )}

      {news.relatedStockSymbols && news.relatedStockSymbols.length > 0 && (
        <div className="news-stock-tags">
          {news.relatedStockSymbols.map((symbol) => (
            <span key={symbol} className="stock-tag">
              {symbol}
            </span>
          ))}
        </div>
      )}
    </div>
  );
};

export default NewsSummaryCard;

