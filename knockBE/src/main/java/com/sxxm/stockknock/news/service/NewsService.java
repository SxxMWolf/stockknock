/**
 * 뉴스 조회 및 관리. 최근 뉴스 조회, AI 분석, 오래된 뉴스 자동 삭제.
 */
package com.sxxm.stockknock.news.service;

import com.sxxm.stockknock.common.service.FastApiService;
import com.sxxm.stockknock.news.dto.NewsAnalysisDto;
import com.sxxm.stockknock.news.dto.NewsDto;
import com.sxxm.stockknock.news.entity.News;
import com.sxxm.stockknock.news.entity.NewsAnalysis;
import com.sxxm.stockknock.news.repository.NewsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class NewsService {

    @Autowired
    private NewsRepository newsRepository;

    @Autowired
    private FastApiService fastApiService;

    /**
     * 최근 뉴스 조회 (DB에서만 조회)
     * 
     * @param days 조회할 일수 (기본값: 7일)
     * @return 최근 뉴스 목록
     */
    public List<NewsDto> getRecentNews(int days) {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(days);
        
        System.out.println("[뉴스 조회] 기간: " + startDate + " ~ " + endDate);
        
        // DB에서 최근 뉴스 조회
        List<News> newsList = newsRepository.findByPublishedAtBetweenOrderByPublishedAtDesc(startDate, endDate);
        System.out.println("[뉴스 조회] 데이터베이스에서 조회된 뉴스 개수: " + newsList.size());
        
        // 뉴스가 없는 경우 30일로 확장 조회 (사용자 경험 개선)
        if (newsList.isEmpty()) {
            System.out.println("[뉴스 조회] 최근 " + days + "일 내 뉴스가 없어서 30일로 확장 조회 시도");
            LocalDateTime extendedStartDate = endDate.minusDays(30);
            newsList = newsRepository.findByPublishedAtBetweenOrderByPublishedAtDesc(extendedStartDate, endDate);
            System.out.println("[뉴스 조회] 30일 확장 조회 결과: " + newsList.size() + "개");
            
            // 여전히 뉴스가 없으면 경고 로그만 출력 (프론트엔드는 빈 배열 반환)
            if (newsList.isEmpty()) {
                System.out.println("[뉴스 조회] 경고: 데이터베이스에 뉴스 데이터가 없습니다.");
            }
        }
        
        return newsList.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    public NewsDto getNewsById(Long newsId) {
        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new RuntimeException("뉴스를 찾을 수 없습니다."));
        return convertToDto(news);
    }

    @Autowired
    private com.sxxm.stockknock.news.repository.NewsAnalysisRepository newsAnalysisRepository;

    public NewsAnalysisDto analyzeNews(Long newsId) {
        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new RuntimeException("뉴스를 찾을 수 없습니다."));

        // 이미 분석이 있으면 반환
        if (news.getAnalysis() != null) {
            NewsAnalysis existingAnalysis = news.getAnalysis();
            return NewsAnalysisDto.builder()
                    .summary(existingAnalysis.getSummary())
                    .impactAnalysis(existingAnalysis.getAiComment())
                    .sentiment(existingAnalysis.getSentiment())
                    .impactScore(existingAnalysis.getImpactScore())
                    .build();
        }

        // FastAPI에서 AI 분석 수행
        try {
            Map<String, Object> analysisResult = fastApiService.analyzeNews(news.getId()).block();
            
            if (analysisResult != null && !analysisResult.isEmpty()) {
                // FastAPI에서 분석 결과를 받아서 저장
                // @MapsId를 사용하지만 newsId를 명시적으로 설정하여 안전하게 처리
                NewsAnalysis analysis = new NewsAnalysis();
                analysis.setNewsId(news.getId()); // newsId를 명시적으로 설정
                analysis.setNews(news); // @MapsId를 위한 news 객체 설정
                analysis.setSummary((String) analysisResult.getOrDefault("summary", ""));
                analysis.setAiComment((String) analysisResult.getOrDefault("ai_comment", ""));
                analysis.setSentiment((String) analysisResult.getOrDefault("sentiment", "neutral"));
                analysis.setImpactScore(((Number) analysisResult.getOrDefault("impact_score", 5)).intValue());
                analysis.setAnalyzedAt(LocalDateTime.now());

                analysis = newsAnalysisRepository.save(analysis);
                news.setAnalysis(analysis);
                newsRepository.save(news);

                return NewsAnalysisDto.builder()
                        .summary(analysis.getSummary())
                        .impactAnalysis(analysis.getAiComment())
                        .sentiment(analysis.getSentiment())
                        .impactScore(analysis.getImpactScore())
                        .build();
            }
        } catch (Exception e) {
            System.err.println("FastAPI 뉴스 분석 실패: " + e.getMessage());
        }
        
        // FastAPI 실패 시 기본값 반환
        return NewsAnalysisDto.builder()
                .summary("분석 중 오류가 발생했습니다.")
                .impactAnalysis("")
                .sentiment("neutral")
                .impactScore(5)
                .build();
    }

    public NewsDto convertToDto(News news) {
        List<String> relatedSymbols = news.getStockRelations() != null ?
                news.getStockRelations().stream()
                        .map(rel -> rel.getStockSymbol())
                        .collect(Collectors.toList()) :
                List.of();

        NewsAnalysisDto analysisDto = null;
        try {
            // Lazy loading을 안전하게 처리
            NewsAnalysis analysis = news.getAnalysis();
            if (analysis != null && analysis.getNewsId() != null) {
            analysisDto = NewsAnalysisDto.builder()
                    .summary(analysis.getSummary())
                    .impactAnalysis(analysis.getAiComment())
                    .sentiment(analysis.getSentiment())
                    .impactScore(analysis.getImpactScore())
                    .build();
            }
        } catch (Exception e) {
            // Lazy loading 실패 시 무시 (분석 데이터가 없는 것으로 처리)
            System.err.println("뉴스 분석 데이터 로딩 실패 (뉴스 ID: " + news.getId() + "): " + e.getMessage());
        }

        return NewsDto.builder()
                .id(news.getId())
                .title(news.getTitle())
                .content(news.getContent())
                .source(news.getSource())
                .url(news.getUrl())
                .publishedAt(news.getPublishedAt())
                .relatedStockSymbols(relatedSymbols)
                .analysis(analysisDto)
                .build();
    }


    @Autowired
    private MarketBriefingService marketBriefingService;

    /**
     * 시장 브리핑 조회 (GPT 호출 없음, DB에서만 조회)
     * 
     * @param token JWT 토큰 (사용하지 않지만 하위 호환성을 위해 유지)
     * @return 시장 브리핑 요약
     */
    public String getMarketBriefing(String token) {
        // MarketBriefingService를 통해 조회 (GPT 호출 없음)
        return marketBriefingService.getTodayGlobalBriefing();
    }
    



    /**
     * 일정 기간 이상 된 뉴스 삭제
     * @param days 삭제할 뉴스의 최소 보관 기간 (일)
     * @return 삭제된 뉴스 개수
     */
    public int deleteNewsOlderThan(int days) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
        
        System.out.println("[뉴스 삭제] " + days + "일 이상 된 뉴스 삭제 시작 (기준일: " + cutoffDate + ")");
        
        // 삭제할 뉴스 조회
        List<News> oldNews = newsRepository.findByPublishedAtBefore(cutoffDate);
        int count = oldNews.size();
        
        if (count > 0) {
            System.out.println("[뉴스 삭제] 삭제 대상 뉴스 개수: " + count);
            
            // 뉴스 삭제 (CASCADE로 관련 분석 데이터도 자동 삭제됨)
            newsRepository.deleteByPublishedAtBefore(cutoffDate);
            
            System.out.println("[뉴스 삭제] " + count + "개의 오래된 뉴스가 삭제되었습니다.");
        } else {
            System.out.println("[뉴스 삭제] 삭제할 뉴스가 없습니다.");
        }
        
        return count;
    }
}

