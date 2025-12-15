package com.sxxm.stockknock.news.repository;

import com.sxxm.stockknock.news.entity.News;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NewsRepository extends JpaRepository<News, Long> {
    List<News> findByPublishedAtBetweenOrderByPublishedAtDesc(LocalDateTime start, LocalDateTime end);
    List<News> findByTitleContainingOrContentContaining(String title, String content);
}

