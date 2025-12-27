package com.sxxm.stockknock.news.repository;

/**
 * 뉴스 레포지토리
 * 
 * 역할:
 * - 뉴스 엔티티의 데이터베이스 접근
 * - 기간별 뉴스 조회
 * - 제목/내용으로 뉴스 검색
 * - 오래된 뉴스 삭제
 */
import com.sxxm.stockknock.news.entity.News;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NewsRepository extends JpaRepository<News, Long> {
    List<News> findByPublishedAtBetweenOrderByPublishedAtDesc(LocalDateTime start, LocalDateTime end);
    List<News> findByTitleContainingOrContentContaining(String title, String content);
    
    /**
     * 일정 기간 이전에 발행된 뉴스 조회
     * @param beforeDate 이 날짜 이전의 뉴스
     * @return 해당 날짜 이전의 뉴스 목록
     */
    List<News> findByPublishedAtBefore(LocalDateTime beforeDate);
    
    /**
     * 일정 기간 이전에 발행된 뉴스 삭제
     * @param beforeDate 이 날짜 이전의 뉴스
     */
    void deleteByPublishedAtBefore(LocalDateTime beforeDate);
}

