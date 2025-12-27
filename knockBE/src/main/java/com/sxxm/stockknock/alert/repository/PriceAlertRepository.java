package com.sxxm.stockknock.alert.repository;

/**
 * 가격 알림 레포지토리
 * 
 * 역할:
 * - 가격 알림 엔티티의 데이터베이스 접근
 * - 사용자별 알림 조회
 * - 미발송 알림 조회 (트리거 체크용)
 */
import com.sxxm.stockknock.alert.entity.PriceAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PriceAlertRepository extends JpaRepository<PriceAlert, Long> {
    List<PriceAlert> findByUserId(Long userId);
    List<PriceAlert> findByStockSymbolAndTriggeredFalse(String stockSymbol);
}

