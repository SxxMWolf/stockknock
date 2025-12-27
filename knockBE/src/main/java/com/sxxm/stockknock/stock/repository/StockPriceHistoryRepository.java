package com.sxxm.stockknock.stock.repository;

/**
 * 주식 가격 이력 레포지토리
 * 
 * 역할:
 * - 주식 가격 이력 엔티티의 데이터베이스 접근
 * - 종목별 최신 가격 조회
 * - 여러 종목의 최신 가격을 한 번에 조회 (N+1 Query 방지)
 * - 기간별 가격 이력 조회
 */
import com.sxxm.stockknock.stock.entity.StockPriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockPriceHistoryRepository extends JpaRepository<StockPriceHistory, Long> {
    Optional<StockPriceHistory> findTopByStockSymbolOrderByTimestampDesc(String stockSymbol);
    List<StockPriceHistory> findByStockSymbolOrderByTimestampDesc(String stockSymbol, org.springframework.data.domain.Pageable pageable);
    List<StockPriceHistory> findByStockSymbolAndTimestampBetween(String stockSymbol, LocalDateTime start, LocalDateTime end);
    
    /**
     * 여러 종목의 최신 가격을 한 번에 조회 (N+1 Query 방지)
     * 각 symbol별로 가장 최근 timestamp를 가진 가격을 반환
     */
    @Query(value = """
        SELECT DISTINCT ON (sph.stock_symbol) 
               sph.stock_symbol as symbol, 
               sph.price as price
        FROM stock_price_history sph
        WHERE sph.stock_symbol IN :symbols
        ORDER BY sph.stock_symbol, sph.timestamp DESC
        """, nativeQuery = true)
    List<Object[]> findLatestPricesBySymbols(@Param("symbols") List<String> symbols);
}

