package com.sxxm.stockknock.portfolio.repository;

/**
 * 포트폴리오 레포지토리
 * 
 * 역할:
 * - 포트폴리오 엔티티의 데이터베이스 접근
 * - 사용자별 포트폴리오 조회
 */
import com.sxxm.stockknock.portfolio.entity.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {
    List<Portfolio> findByUserId(Long userId);
}

