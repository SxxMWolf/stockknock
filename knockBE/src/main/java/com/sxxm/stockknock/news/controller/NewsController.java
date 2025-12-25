package com.sxxm.stockknock.news.controller;

import com.sxxm.stockknock.news.dto.NewsAnalysisDto;
import com.sxxm.stockknock.news.dto.NewsDto;
import com.sxxm.stockknock.news.service.NewsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/news")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class NewsController {

    @Autowired
    private NewsService newsService;

    @Autowired
    private com.sxxm.stockknock.news.service.NewsCrawlerService newsCrawlerService;

    /**
     * 최근 뉴스 조회 API
     * 
     * 프론트엔드 요청 시 DB에 저장된 뉴스만 반환합니다.
     * 외부 API를 직접 호출하지 않으며, 정기적으로 수집된 데이터를 사용합니다.
     * 
     * @param days 조회할 일수 (기본값: 7일)
     * @return 최근 뉴스 목록
     */
    @GetMapping("/recent")
    public ResponseEntity<List<NewsDto>> getRecentNews(@RequestParam(defaultValue = "7") int days) {
        System.out.println("[뉴스 API] 프론트엔드 요청 - days: " + days);
        List<NewsDto> news = newsService.getRecentNews(days);
        System.out.println("[뉴스 API] 반환된 뉴스 개수: " + news.size());
        return ResponseEntity.ok(news);
    }

    @GetMapping("/{newsId}")
    public ResponseEntity<NewsDto> getNewsById(@PathVariable Long newsId) {
        try {
            NewsDto news = newsService.getNewsById(newsId);
            return ResponseEntity.ok(news);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{newsId}/analyze")
    public ResponseEntity<NewsAnalysisDto> analyzeNews(@PathVariable Long newsId) {
        try {
            NewsAnalysisDto analysis = newsService.analyzeNews(newsId);
            return ResponseEntity.ok(analysis);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 수동 뉴스 수집 API (개발/관리자용)
     * 
     * ⚠️ 주의: 이 API는 개발 및 운영 편의를 위해 제공됩니다.
     * 일반 사용자 흐름에서는 사용하지 않으며, 다음 상황에서만 사용합니다:
     * - 테스트 환경에서 즉시 뉴스 수집이 필요한 경우
     * - 배포 직후 DB가 비어 있는 상황
     * - 스케줄러 실행 전 수동으로 뉴스를 수집해야 하는 경우
     * 
     * 정기적인 뉴스 수집은 스케줄러가 자동으로 수행합니다.
     * 
     * @return 수집 결과 메시지
     */
    @PostMapping("/collect")
    public ResponseEntity<Map<String, String>> collectNews() {
        try {
            System.out.println("[수동 수집] 뉴스 수집 시작 (개발/관리자용)...");
            newsCrawlerService.collectGeneralStockNews();
            
            // 수집된 뉴스 중 주요 뉴스 자동 분석
            System.out.println("[수동 수집] 주요 뉴스 AI 분석 시작...");
            newsService.analyzeTopNews(20);
            
            System.out.println("[수동 수집] 뉴스 수집 및 분석 완료");
            return ResponseEntity.ok(Map.of("message", "뉴스 수집 및 분석이 완료되었습니다."));
        } catch (Exception e) {
            System.err.println("[수동 수집] 뉴스 수집 오류: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 오늘의 주요 뉴스 5줄 요약
     */
    @GetMapping("/today-summary")
    public ResponseEntity<Map<String, String>> getTodaySummary() {
        try {
            String summary = newsService.summarizeTodayNews();
            return ResponseEntity.ok(Map.of("summary", summary));
        } catch (Exception e) {
            System.err.println("[뉴스 요약] 오류: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

}
