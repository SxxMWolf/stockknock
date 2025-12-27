package com.sxxm.stockknock.news.scheduler;

import com.sxxm.stockknock.news.service.MarketBriefingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 시장 브리핑 생성 스케줄러
 * 
 * 역할:
 * - 매일 평일 08:50에 오늘의 시장 브리핑 자동 생성
 * - 하루 1회만 생성 (중복 방지)
 * - MarketBriefingService를 호출하여 GPT로 브리핑 생성
 * - 주말/공휴일에는 실행하지 않음
 */
@Component
public class MarketBriefingScheduler {

    private static final Logger log = LoggerFactory.getLogger(MarketBriefingScheduler.class);

    @Autowired
    private MarketBriefingService marketBriefingService;

    private LocalDate lastGeneratedDate = null;

    /**
     * 매일 평일 08:50에 시장 브리핑 자동 생성
     * 하루 1회만 생성 (중복 방지)
     */
    @Scheduled(cron = "0 50 8 * * MON-FRI") // 평일 오전 8시 50분
    public void generateDailyMarketBriefing() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        LocalDate today = now.toLocalDate();
        DayOfWeek dayOfWeek = now.getDayOfWeek();

        // 평일이고 오늘 아직 생성하지 않았으면 실행
        if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY &&
            (lastGeneratedDate == null || !lastGeneratedDate.equals(today))) {
            
            log.info("[시장 브리핑 스케줄러] 오늘의 시장 브리핑 생성 시작 (date: {})", today);
            
            try {
                boolean success = marketBriefingService.generateTodayGlobalBriefing();
                if (success) {
                    lastGeneratedDate = today;
                    log.info("[시장 브리핑 스케줄러] 오늘의 시장 브리핑 생성 완료 (date: {})", today);
                } else {
                    log.warn("[시장 브리핑 스케줄러] 오늘의 시장 브리핑 생성 실패 (date: {})", today);
                }
            } catch (Exception e) {
                log.error("[시장 브리핑 스케줄러] 브리핑 생성 중 예외 발생 (date: {}): {}", today, e.getMessage(), e);
            }
        } else {
            log.debug("[시장 브리핑 스케줄러] 오늘 이미 생성됨 또는 주말 - 스킵 (date: {})", today);
        }
    }
}

