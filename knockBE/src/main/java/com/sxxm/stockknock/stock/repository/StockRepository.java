package com.sxxm.stockknock.stock.repository;

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

