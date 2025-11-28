package com.sxxm.stockknock.repository;

import com.sxxm.stockknock.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {
    Optional<Stock> findBySymbol(String symbol);
    List<Stock> findByCountry(String country);
    List<Stock> findByNameContaining(String name);
    List<Stock> findByIndustryId(Long industryId);
}

