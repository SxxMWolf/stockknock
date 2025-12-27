package com.sxxm.stockknock.news.scheduler;

import com.sxxm.stockknock.news.service.NewsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 뉴스 관련 스케줄러
 * 
 * 자동 삭제: 매일 실행
 * - 일주일 이상 된 뉴스 자동 삭제 (DB 부하 방지)
 * 
 * NewsAPI 수집 스케줄러는 제거되었습니다.
 * 뉴스는 GPT-only 시장 브리핑으로 대체되었습니다.
 */
@Component
public class NewsCrawlerScheduler {

    @Autowired
    private NewsService newsService;

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

