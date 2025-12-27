package com.sxxm.stockknock.watchlist.repository;

/**
 * 관심 종목 레포지토리
 * 
 * 역할:
 * - 관심 종목 엔티티의 데이터베이스 접근
 * - 사용자별, 종목별 관심 종목 조회
 */
import com.sxxm.stockknock.watchlist.entity.Watchlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WatchlistRepository extends JpaRepository<Watchlist, com.sxxm.stockknock.watchlist.entity.WatchlistId> {
    List<Watchlist> findByUserId(Long userId);
    List<Watchlist> findByStockSymbol(String stockSymbol);
}

