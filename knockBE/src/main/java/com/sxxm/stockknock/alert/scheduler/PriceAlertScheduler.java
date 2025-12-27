package com.sxxm.stockknock.alert.scheduler;

import com.sxxm.stockknock.alert.entity.PriceAlert;
import com.sxxm.stockknock.stock.entity.Stock;
import com.sxxm.stockknock.alert.repository.PriceAlertRepository;
import com.sxxm.stockknock.stock.repository.StockPriceHistoryRepository;
import com.sxxm.stockknock.alert.service.NotificationService;
import com.sxxm.stockknock.common.service.FastApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 가격 알림 체크 스케줄러
 * 
 * 역할:
 * - 주기적으로 모든 가격 알림을 체크
 * - 목표가에 도달한 알림을 찾아서 알림 발송
 * - 알림 발송 후 알림 상태 업데이트 또는 삭제
 */
@Component
public class PriceAlertScheduler {

    @Autowired
    private PriceAlertRepository priceAlertRepository;

    @Autowired
    private StockPriceHistoryRepository priceHistoryRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private FastApiService fastApiService;

    /**
     * 매 30초마다 가격 알림 체크
     */
    @Scheduled(fixedRate = 30000) // 30초마다
    public void checkPriceAlerts() {
        List<PriceAlert> activeAlerts = priceAlertRepository.findAll()
                .stream()
                .filter(alert -> !alert.getTriggered())
                .toList();

        for (PriceAlert alert : activeAlerts) {
            Stock stock = alert.getStock();
            
            // FastAPI에서 최신 가격 가져오기
            BigDecimal currentPrice = null;
            BigDecimal previousPrice = null;
            
            try {
                currentPrice = fastApiService.getCurrentPrice(stock.getSymbol()).block();
            } catch (Exception e) {
                System.err.println("FastAPI 주가 조회 실패, DB에서 조회: " + e.getMessage());
                // FastAPI 실패 시 DB에서 조회
                Optional<com.sxxm.stockknock.stock.entity.StockPriceHistory> latestPrice = 
                        priceHistoryRepository.findTopByStockSymbolOrderByTimestampDesc(stock.getSymbol());
                if (latestPrice.isPresent()) {
                    currentPrice = latestPrice.get().getPrice();
                }
            }
            
            if (currentPrice == null) {
                continue;
            }
            
            // 전일 종가 가져오기 (두 번째 최신 가격) - DB에서 조회
            List<com.sxxm.stockknock.stock.entity.StockPriceHistory> priceHistory = 
                    priceHistoryRepository.findByStockSymbolOrderByTimestampDesc(
                            stock.getSymbol(), 
                            org.springframework.data.domain.PageRequest.of(1, 1)
                    );
            if (!priceHistory.isEmpty()) {
                previousPrice = priceHistory.get(0).getPrice();
            }

            boolean shouldTrigger = false;
            String alertMessage = "";

            String alertType = alert.getAlertType();
            if ("TARGET".equals(alertType)) {
                if (alert.getTargetPrice() != null) {
                    if (currentPrice.compareTo(alert.getTargetPrice()) >= 0) {
                        shouldTrigger = true;
                        alertMessage = String.format(
                                "%s 주식이 목표가 %s원에 도달했습니다. 현재가: %s원",
                                stock.getName(), alert.getTargetPrice(), currentPrice
                        );
                    }
                }
            } else if ("STOP_LOSS".equals(alertType)) {
                if (alert.getTargetPrice() != null) {
                    if (currentPrice.compareTo(alert.getTargetPrice()) <= 0) {
                        shouldTrigger = true;
                        alertMessage = String.format(
                                "%s 주식이 손절가 %s원에 도달했습니다. 현재가: %s원",
                                stock.getName(), alert.getTargetPrice(), currentPrice
                        );
                    }
                }
            } else if ("PERCENT".equals(alertType)) {
                if (alert.getPercentChange() != null && previousPrice != null) {
                    BigDecimal change = currentPrice.subtract(previousPrice);
                    BigDecimal changePercent = change
                            .divide(previousPrice, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100));

                    BigDecimal threshold = alert.getPercentChange().abs();
                    if (changePercent.abs().compareTo(threshold) >= 0) {
                        shouldTrigger = true;
                        alertMessage = String.format(
                                "%s 주식이 %s%% 변동했습니다. 현재가: %s원 (전일 대비: %s%%)",
                                stock.getName(), changePercent, currentPrice, changePercent
                        );
                    }
                }
            }

            if (shouldTrigger) {
                alert.setTriggered(true);
                alert.setTriggeredAt(LocalDateTime.now());
                priceAlertRepository.save(alert);

                // 알림 발송
                notificationService.sendPriceAlert(alert.getUser(), alertMessage, stock);
            }
        }
    }
}

