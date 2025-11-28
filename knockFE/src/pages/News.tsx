import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { newsAPI } from '../api/news';
import './News.css';

const News: React.FC = () => {
  const [selectedNews, setSelectedNews] = useState<number | null>(null);
  
  const { data: news, isLoading } = useQuery({
    queryKey: ['recentNews'],
    queryFn: () => newsAPI.getRecent(7),
  });

  const { data: analysis } = useQuery({
    queryKey: ['newsAnalysis', selectedNews],
    queryFn: () => newsAPI.analyze(selectedNews!),
    enabled: selectedNews !== null,
  });

  if (isLoading) return <div>로딩 중...</div>;

  return (
    <div className="news">
      <h1>뉴스</h1>
      <div className="news-list">
        {news?.map((item) => (
          <div 
            key={item.id} 
            className="news-item"
            onClick={() => setSelectedNews(item.id)}
          >
            <h3>{item.title}</h3>
            <p className="source">{item.source} · {new Date(item.publishedAt).toLocaleDateString()}</p>
            <p className="content">{item.content.substring(0, 200)}...</p>
            {item.analysis && (
              <div className="analysis">
                <h4>AI 분석</h4>
                <p>{item.analysis.summary}</p>
                <p className={`sentiment ${item.analysis.sentiment.toLowerCase()}`}>
                  감정: {item.analysis.sentiment} (영향도: {item.analysis.impactScore}/10)
                </p>
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
};

export default News;

