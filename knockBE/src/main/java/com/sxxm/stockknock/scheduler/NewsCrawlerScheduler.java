package com.sxxm.stockknock.scheduler;

import com.sxxm.stockknock.service.NewsCrawlerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 뉴스 수집 스케줄러
 */
@Component
public class NewsCrawlerScheduler {

    @Autowired
    private NewsCrawlerService newsCrawlerService;

    /**
     * 매 1시간마다 뉴스 수집
     */
    @Scheduled(cron = "0 0 * * * *") // 매 시간 정각
    public void collectNews() {
        System.out.println("뉴스 수집 시작...");
        newsCrawlerService.collectGeneralStockNews();
        System.out.println("뉴스 수집 완료");
    }

    /**
     * 매일 오전 9시에 주요 종목 뉴스 수집
     */
    @Scheduled(cron = "0 0 9 * * *") // 매일 9시
    public void collectStockNews() {
        // 주요 종목들에 대한 뉴스 수집
        String[] majorStocks = {"AAPL", "MSFT", "GOOGL", "TSLA", "005930", "000660"};
        for (String symbol : majorStocks) {
            newsCrawlerService.collectNewsForStock(symbol);
        }
    }
}

