/**
 * 포트폴리오 분석 레포지토리. 사용자별 분석 결과 조회.
 */
package com.sxxm.stockknock.portfolio.repository;

import com.sxxm.stockknock.portfolio.entity.PortfolioAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PortfolioAnalysisRepository extends JpaRepository<PortfolioAnalysis, Long> {
    Optional<PortfolioAnalysis> findByUserId(Long userId);
}

