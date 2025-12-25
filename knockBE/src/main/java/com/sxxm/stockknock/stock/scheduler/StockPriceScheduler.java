package com.sxxm.stockknock.stock.scheduler;

import com.sxxm.stockknock.stock.service.StockPriceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * 주식 가격을 주기적으로 업데이트하는 스케줄러
 * - 장중(09:00~15:30): 5~10분 주기
 * - 장 마감 후: 30~60분 주기
 * - 주말/공휴일: 갱신 안 함
 */
@Component
public class StockPriceScheduler {

    @Autowired
    private StockPriceService stockPriceService;

    /**
     * 장중 주식 가격 업데이트 (09:00~15:30, 5~10분 주기)
     * 실제로는 7분마다 실행 (5~10분의 중간값)
     */
    @Scheduled(cron = "0 */7 9-15 * * MON-FRI")
    public void updateStockPricesDuringMarketHours() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        LocalTime currentTime = now.toLocalTime();
        
        // 장중 시간 체크 (09:00~15:30)
        if (currentTime.isAfter(LocalTime.of(9, 0)) && 
            currentTime.isBefore(LocalTime.of(15, 31))) {
            System.out.println("장중 주식 가격 업데이트 시작... (" + now + ")");
            stockPriceService.updateAllStockPrices();
            System.out.println("장중 주식 가격 업데이트 완료");
        }
    }

    /**
     * 장 마감 후 주식 가격 업데이트 (15:30 이후, 30~60분 주기)
     * 실제로는 45분마다 실행 (30~60분의 중간값)
     */
    @Scheduled(cron = "0 */45 * * * MON-FRI")
    public void updateStockPricesAfterMarketClose() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        LocalTime currentTime = now.toLocalTime();
        DayOfWeek dayOfWeek = now.getDayOfWeek();
        
        // 평일이고 장 마감 후인 경우만 업데이트
        if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY &&
            currentTime.isAfter(LocalTime.of(15, 30))) {
            System.out.println("장 마감 후 주식 가격 업데이트 시작... (" + now + ")");
            stockPriceService.updateAllStockPrices();
            System.out.println("장 마감 후 주식 가격 업데이트 완료");
        }
    }
}

