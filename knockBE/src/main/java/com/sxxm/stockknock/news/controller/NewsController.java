/**
 * 뉴스 및 시장 브리핑 API. 최근 뉴스 조회, AI 분석, 시장 브리핑 조회.
 */
package com.sxxm.stockknock.news.controller;

import com.sxxm.stockknock.news.dto.NewsAnalysisDto;
import com.sxxm.stockknock.news.dto.NewsDto;
import com.sxxm.stockknock.news.service.MarketBriefingService;
import com.sxxm.stockknock.news.service.NewsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/news")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class NewsController {

    private static final Logger log = LoggerFactory.getLogger(NewsController.class);

    @Autowired
    private NewsService newsService;

    @Autowired
    private MarketBriefingService marketBriefingService;

    /**
     * 최근 뉴스 조회 API
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
     * 시장 브리핑 조회 (GPT 호출 없음, DB에서만 조회)
     * 스케줄러가 매일 08:50에 생성한 브리핑을 반환합니다.
     * 
     * @param token JWT 토큰 (하위 호환성을 위해 유지하지만 실제로는 사용하지 않음)
     * @return 시장 브리핑 요약
     */
    @GetMapping("/market-briefing")
    public ResponseEntity<Map<String, String>> getMarketBriefing(
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            // GPT 호출 없이 DB에서만 조회
            String summary = marketBriefingService.getTodayGlobalBriefing();
            return ResponseEntity.ok(Map.of("summary", summary));
        } catch (Exception e) {
            log.error("[시장 브리핑] 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", "시장 브리핑 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 오늘의 주요 뉴스 5줄 요약 (하위 호환성)
     * 내부적으로 market-briefing 호출
     */
    @GetMapping("/today-summary")
    public ResponseEntity<Map<String, String>> getTodaySummary(@RequestHeader(value = "Authorization", required = false) String token) {
        return getMarketBriefing(token);
    }

}
