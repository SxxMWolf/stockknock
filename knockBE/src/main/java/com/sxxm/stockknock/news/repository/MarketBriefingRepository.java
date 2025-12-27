package com.sxxm.stockknock.news.repository;

/**
 * 시장 브리핑 레포지토리
 * 
 * 역할:
 * - 시장 브리핑 엔티티의 데이터베이스 접근
 * - 사용자별, 날짜별 브리핑 조회
 * - 전역 브리핑 (userId = 0) 조회 지원
 */
import com.sxxm.stockknock.news.entity.MarketBriefing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface MarketBriefingRepository extends JpaRepository<MarketBriefing, Long> {
    Optional<MarketBriefing> findByUserIdAndDate(Long userId, LocalDate date);
}

