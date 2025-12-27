package com.sxxm.stockknock.portfolio.repository;

/**
 * 포트폴리오 항목 레포지토리
 * 
 * 역할:
 * - 포트폴리오 항목 엔티티의 데이터베이스 접근
 * - 포트폴리오별, 사용자별 항목 조회
 * - JOIN FETCH를 사용한 N+1 Query 방지
 */
import com.sxxm.stockknock.portfolio.entity.PortfolioItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PortfolioItemRepository extends JpaRepository<PortfolioItem, Long> {
    List<PortfolioItem> findByPortfolioId(Long portfolioId);
    Optional<PortfolioItem> findByPortfolioIdAndStockSymbol(Long portfolioId, String stockSymbol);
    
    @Query("SELECT pi FROM PortfolioItem pi " +
           "JOIN FETCH pi.stock " +
           "JOIN FETCH pi.portfolio p " +
           "WHERE p.user.id = :userId")
    List<PortfolioItem> findByPortfolioUserId(@Param("userId") Long userId);
}

