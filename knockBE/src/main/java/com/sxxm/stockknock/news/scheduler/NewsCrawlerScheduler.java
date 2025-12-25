package com.sxxm.stockknock.news.scheduler;

import com.sxxm.stockknock.news.service.NewsCrawlerService;
import com.sxxm.stockknock.news.service.NewsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 뉴스 수집 및 분석 스케줄러
 * 
 * 정기 자동 수집: 매일 1회 실행
 * - 오전 8시 (장 시작 1시간 전): 주식 뉴스 수집 및 분석
 * 
 * 자동 삭제: 매일 실행
 * - 일주일 이상 된 뉴스 자동 삭제 (DB 부하 방지)
 * 
 * 수집된 뉴스는 자동으로 AI 분석을 수행하고 DB에 저장됩니다.
 * 프론트엔드는 항상 DB에 저장된 뉴스만 조회합니다.
 */
@Component
public class NewsCrawlerScheduler {

    @Autowired
    private NewsCrawlerService newsCrawlerService;

    @Autowired
    private NewsService newsService;

    /**
     * 매일 오전 8시 (장 시작 1시간 전) - 주식 뉴스 수집 및 분석
     * 한국 시장 개장 전 주식 관련 뉴스를 수집하여 사용자에게 제공
     */
    @Scheduled(cron = "0 0 8 * * *") // 매일 오전 8시
    public void collectDailyNews() {
        System.out.println("========== 일일 뉴스 수집 시작 (08:00) ==========");
        
        try {
            // 일반 주식 뉴스 수집
            System.out.println("주식 뉴스 수집 중...");
            newsCrawlerService.collectGeneralStockNews();
            System.out.println("주식 뉴스 수집 완료");
            
            // 주요 종목들에 대한 뉴스 수집
            String[] majorStocks = {"AAPL", "MSFT", "GOOGL", "TSLA", "AMZN", "005930", "000660", "035420"};
            int collectedCount = 0;
            
            for (String symbol : majorStocks) {
                System.out.println("종목 뉴스 수집 중: " + symbol);
                newsCrawlerService.collectNewsForStock(symbol);
                collectedCount++;
            }
            
            System.out.println("주요 종목 뉴스 수집 완료: " + collectedCount + "개 종목");
            
            // 수집된 뉴스 중 주요 뉴스 자동 분석 (비용 최적화: 상위 20개만)
            System.out.println("주요 뉴스 AI 분석 시작 (최대 20개)...");
            newsService.analyzeTopNews(20);
            System.out.println("주요 뉴스 AI 분석 완료");
            
        } catch (Exception e) {
            System.err.println("뉴스 수집 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("========== 일일 뉴스 수집 완료 ==========");
    }

    /**
     * 매일 오전 8시 5분 - 일주일 이상 된 뉴스 자동 삭제
     * DB 부하 방지를 위해 오래된 뉴스를 정리
     */
    @Scheduled(cron = "0 5 8 * * *") // 매일 오전 8시 5분
    public void deleteOldNews() {
        System.out.println("========== 오래된 뉴스 삭제 시작 ==========");
        
        try {
            int deletedCount = newsService.deleteNewsOlderThan(7); // 7일 이상 된 뉴스 삭제
            System.out.println("오래된 뉴스 삭제 완료: " + deletedCount + "개");
        } catch (Exception e) {
            System.err.println("오래된 뉴스 삭제 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("========== 오래된 뉴스 삭제 완료 ==========");
    }
}

