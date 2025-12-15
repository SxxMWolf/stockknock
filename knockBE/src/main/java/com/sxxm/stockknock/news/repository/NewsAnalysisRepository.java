package com.sxxm.stockknock.news.repository;

import com.sxxm.stockknock.news.entity.NewsAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NewsAnalysisRepository extends JpaRepository<NewsAnalysis, Long> {
    Optional<NewsAnalysis> findByNewsId(Long newsId);
    Optional<NewsAnalysis> findByNews(com.sxxm.stockknock.news.entity.News news);
}

