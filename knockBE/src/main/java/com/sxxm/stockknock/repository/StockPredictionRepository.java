package com.sxxm.stockknock.repository;

import com.sxxm.stockknock.entity.StockPrediction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockPredictionRepository extends JpaRepository<StockPrediction, Long> {
    List<StockPrediction> findByStockId(Long stockId);
    Optional<StockPrediction> findTopByStockIdOrderByCreatedAtDesc(Long stockId);
}

