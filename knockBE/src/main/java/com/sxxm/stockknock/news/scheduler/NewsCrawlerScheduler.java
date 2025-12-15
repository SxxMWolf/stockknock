package com.sxxm.stockknock.news.scheduler;

import com.sxxm.stockknock.news.service.NewsCrawlerService;
import com.sxxm.stockknock.news.service.NewsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 뉴스 수집 및 분석 스케줄러
 */
@Component
public class NewsCrawlerScheduler {

    @Autowired
    private NewsCrawlerService newsCrawlerService;

    @Autowired
    private NewsService newsService;

    /**
     * 매 2시간마다 뉴스 수집 및 주요 뉴스 자동 분석
     */
    @Scheduled(cron = "0 0 */2 * * *") // 매 2시간마다 정각
    public void collectAndAnalyzeNews() {
        System.out.println("뉴스 수집 시작...");
        newsCrawlerService.collectGeneralStockNews();
        System.out.println("뉴스 수집 완료");

        // 주요 20개 뉴스만 자동 분석 (비용 최적화)
        System.out.println("주요 뉴스 자동 분석 시작...");
        newsService.analyzeTopNews(20);
        System.out.println("주요 뉴스 자동 분석 완료");
    }

    /**
     * 매일 오전 9시에 주요 종목 뉴스 수집 및 분석
     */
    @Scheduled(cron = "0 0 9 * * *") // 매일 9시
    public void collectStockNews() {
        // 주요 종목들에 대한 뉴스 수집
        String[] majorStocks = {"AAPL", "MSFT", "GOOGL", "TSLA", "005930", "000660"};
        for (String symbol : majorStocks) {
            newsCrawlerService.collectNewsForStock(symbol);
        }

        // 주요 종목 뉴스도 분석
        System.out.println("주요 종목 뉴스 자동 분석 시작...");
        newsService.analyzeTopNews(20);
        System.out.println("주요 종목 뉴스 자동 분석 완료");
    }
}

