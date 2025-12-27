package com.sxxm.stockknock.stock.repository;

/**
 * 주식 레포지토리
 * 
 * 역할:
 * - 주식 엔티티의 데이터베이스 접근
 * - 심볼, 이름, 국가, 산업별로 주식 조회
 * - 종목 검색 기능 지원
 */
import com.sxxm.stockknock.stock.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<Stock, String> {
    Optional<Stock> findBySymbol(String symbol);
    List<Stock> findByCountry(String country);
    List<Stock> findByNameContaining(String name);
    List<Stock> findByIndustry(String industry);
}

