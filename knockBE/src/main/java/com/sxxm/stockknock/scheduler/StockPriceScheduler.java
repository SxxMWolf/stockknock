package com.sxxm.stockknock.scheduler;

import com.sxxm.stockknock.service.StockPriceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 주식 가격을 주기적으로 업데이트하는 스케줄러
 */
@Component
public class StockPriceScheduler {

    @Autowired
    private StockPriceService stockPriceService;

    /**
     * 매 1분마다 주식 가격 업데이트
     * cron 표현식: 초 분 시 일 월 요일
     */
    @Scheduled(cron = "0 */1 * * * *") // 매 1분
    public void updateStockPrices() {
        System.out.println("주식 가격 업데이트 시작...");
        stockPriceService.updateAllStockPrices();
        System.out.println("주식 가격 업데이트 완료");
    }

    /**
     * 시장 개장 시간에만 업데이트 (예: 한국 시간 09:00-15:30, 미국 시간 22:30-05:00)
     * 주식 시장이 열려있을 때만 실행
     */
    @Scheduled(cron = "0 */5 9-15 * * MON-FRI") // 한국 시장: 평일 9시-15시 5분마다
    public void updateStockPricesDuringMarketHours() {
        stockPriceService.updateAllStockPrices();
    }
}

