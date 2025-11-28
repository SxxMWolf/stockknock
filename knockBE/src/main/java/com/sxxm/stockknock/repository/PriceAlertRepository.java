package com.sxxm.stockknock.repository;

import com.sxxm.stockknock.entity.PriceAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PriceAlertRepository extends JpaRepository<PriceAlert, Long> {
    List<PriceAlert> findByUserId(Long userId);
    List<PriceAlert> findByStockIdAndIsTriggeredFalse(Long stockId);
}

