package com.sxxm.stockknock.news.repository;

import com.sxxm.stockknock.news.entity.NewsStockRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NewsStockRelationRepository extends JpaRepository<NewsStockRelation, com.sxxm.stockknock.news.entity.NewsStockRelationId> {
    List<NewsStockRelation> findByNewsId(Long newsId);
    List<NewsStockRelation> findByStockSymbol(String stockSymbol);
}

