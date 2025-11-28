package com.sxxm.stockknock.scheduler;

import com.sxxm.stockknock.entity.PriceAlert;
import com.sxxm.stockknock.entity.Stock;
import com.sxxm.stockknock.repository.PriceAlertRepository;
import com.sxxm.stockknock.repository.StockRepository;
import com.sxxm.stockknock.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 가격 알림을 체크하고 트리거하는 스케줄러
 */
@Component
public class PriceAlertScheduler {

    @Autowired
    private PriceAlertRepository priceAlertRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private NotificationService notificationService;

    /**
     * 매 10초마다 가격 알림 체크
     */
    @Scheduled(fixedRate = 10000) // 10초마다
    public void checkPriceAlerts() {
        List<PriceAlert> activeAlerts = priceAlertRepository.findAll()
                .stream()
                .filter(alert -> !alert.getIsTriggered())
                .toList();

        for (PriceAlert alert : activeAlerts) {
            Stock stock = alert.getStock();
            
            // 최신 가격으로 업데이트 (스케줄러가 이미 업데이트했을 수 있음)
            stock = stockRepository.findById(stock.getId()).orElse(null);
            if (stock == null || stock.getCurrentPrice() == null) {
                continue;
            }

            BigDecimal currentPrice = stock.getCurrentPrice();
            boolean shouldTrigger = false;
            String alertMessage = "";

            switch (alert.getAlertType()) {
                case TARGET_PRICE:
                    if (alert.getTargetPrice() != null) {
                        if (currentPrice.compareTo(alert.getTargetPrice()) >= 0) {
                            shouldTrigger = true;
                            alertMessage = String.format(
                                    "%s 주식이 목표가 %s원에 도달했습니다. 현재가: %s원",
                                    stock.getName(), alert.getTargetPrice(), currentPrice
                            );
                        }
                    }
                    break;

                case STOP_LOSS:
                    if (alert.getTargetPrice() != null) {
                        if (currentPrice.compareTo(alert.getTargetPrice()) <= 0) {
                            shouldTrigger = true;
                            alertMessage = String.format(
                                    "%s 주식이 손절가 %s원에 도달했습니다. 현재가: %s원",
                                    stock.getName(), alert.getTargetPrice(), currentPrice
                            );
                        }
                    }
                    break;

                case PERCENTAGE_CHANGE:
                    if (alert.getPercentageChange() != null && stock.getPreviousClose() != null) {
                        BigDecimal change = currentPrice.subtract(stock.getPreviousClose());
                        BigDecimal changePercent = change
                                .divide(stock.getPreviousClose(), 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100));

                        BigDecimal threshold = alert.getPercentageChange().abs();
                        if (changePercent.abs().compareTo(threshold) >= 0) {
                            shouldTrigger = true;
                            alertMessage = String.format(
                                    "%s 주식이 %s%% 변동했습니다. 현재가: %s원 (전일 대비: %s%%)",
                                    stock.getName(), changePercent, currentPrice, changePercent
                            );
                        }
                    }
                    break;
            }

            if (shouldTrigger) {
                alert.setIsTriggered(true);
                alert.setTriggeredAt(LocalDateTime.now());
                priceAlertRepository.save(alert);

                // 알림 발송
                notificationService.sendPriceAlert(alert.getUser(), alertMessage, stock);
            }
        }
    }
}

