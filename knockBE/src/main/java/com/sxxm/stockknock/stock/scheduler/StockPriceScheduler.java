package com.sxxm.stockknock.stock.scheduler;

import com.sxxm.stockknock.stock.service.StockPriceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * 주식 가격 업데이트 스케줄러
 * 
 * 역할:
 * - 장 종료 후 주식 가격 일괄 업데이트 (하루 1회)
 * - 장중(09:00~15:30)에는 가격 조회 비활성화 (캐시만 사용)
 * - 평일만 실행 (주말/공휴일 제외)
 * - 16시~23시 매 시간 정각에 체크하되, 오늘 이미 업데이트했으면 스킵
 */
@Component
public class StockPriceScheduler {

    @Autowired
    private StockPriceService stockPriceService;
    
    // 마지막 업데이트 날짜 (하루 1회 체크용)
    private LocalDate lastUpdateDate = null;

    /**
     * 장 종료 후 주식 가격 업데이트 (15:30 이후, 하루 1회)
     * 매 시간마다 체크하되, 오늘 이미 업데이트했으면 스킵
     */
    @Scheduled(cron = "0 0 16-23 * * MON-FRI")  // 16시~23시 매 시간 정각에 체크
    public void updateStockPricesAfterMarketClose() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        LocalDate today = now.toLocalDate();
        LocalTime currentTime = now.toLocalTime();
        DayOfWeek dayOfWeek = now.getDayOfWeek();
        
        // 평일이고 장 마감 후(15:30 이후)이고, 오늘 아직 업데이트하지 않았으면 실행
        if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY &&
            currentTime.isAfter(LocalTime.of(15, 30)) &&
            (lastUpdateDate == null || !lastUpdateDate.equals(today))) {
            System.out.println("[PRICE] 장 종료 후 가격 갱신 시작... (" + now + ")");
            stockPriceService.updateAllStockPrices();
            lastUpdateDate = today;  // 오늘 날짜로 업데이트
            System.out.println("[PRICE] 장 종료 후 가격 갱신 완료 (하루 1회)");
        }
    }
}

