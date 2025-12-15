package com.sxxm.stockknock.watchlist.repository;

import com.sxxm.stockknock.watchlist.entity.Watchlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WatchlistRepository extends JpaRepository<Watchlist, com.sxxm.stockknock.watchlist.entity.WatchlistId> {
    List<Watchlist> findByUserId(Long userId);
    List<Watchlist> findByStockSymbol(String stockSymbol);
}

