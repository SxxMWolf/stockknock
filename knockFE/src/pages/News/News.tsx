import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { newsAPI } from '../../api/news';
import './News.css';

const News: React.FC = () => {
  const navigate = useNavigate();
  const { data: news, isLoading, error } = useQuery({
    queryKey: ['recentNews'],
    queryFn: () => newsAPI.getRecent(7),
  });

  if (isLoading) return <div className="news-loading">로딩 중...</div>;
  if (error) {
    console.error('뉴스 로딩 에러:', error);
    return <div className="news-error">뉴스를 불러오는 중 오류가 발생했습니다.</div>;
  }

  return (
    <div className="news">
      <h1>뉴스</h1>
      <div className="news-list">
        {news && news.length > 0 ? news.map((item) => (
          <div 
            key={item.id} 
            className="news-item"
            onClick={() => navigate(`/news/${item.id}`)}
          >
            <h3>{item.title}</h3>
            <p className="source">{item.source} · {new Date(item.publishedAt || '').toLocaleDateString()}</p>
            <p className="content">{item.content.substring(0, 200)}...</p>
            {item.analysis && (
              <div className="analysis">
                <h4>AI 분석</h4>
                <p>{item.analysis.summary}</p>
                <p className={`sentiment ${item.analysis.sentiment?.toLowerCase() || ''}`}>
                  감정: {item.analysis.sentiment} (영향도: {item.analysis.impactScore}/10)
                </p>
              </div>
            )}
            <button className="btn-read-more">자세히 보기 →</button>
          </div>
        )) : (
          <div className="news-empty">표시할 뉴스가 없습니다.</div>
        )}
      </div>
    </div>
  );
};

export default News;

