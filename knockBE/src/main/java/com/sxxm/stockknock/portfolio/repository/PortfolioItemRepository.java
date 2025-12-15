package com.sxxm.stockknock.portfolio.repository;

import com.sxxm.stockknock.portfolio.entity.PortfolioItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PortfolioItemRepository extends JpaRepository<PortfolioItem, Long> {
    List<PortfolioItem> findByPortfolioId(Long portfolioId);
    Optional<PortfolioItem> findByPortfolioIdAndStockSymbol(Long portfolioId, String stockSymbol);
    List<PortfolioItem> findByPortfolioUserId(Long userId);
}

