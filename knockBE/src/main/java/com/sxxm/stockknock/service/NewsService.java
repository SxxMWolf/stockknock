package com.sxxm.stockknock.service;

import com.sxxm.stockknock.ai.AIService;
import com.sxxm.stockknock.dto.NewsAnalysisDto;
import com.sxxm.stockknock.dto.NewsDto;
import com.sxxm.stockknock.entity.News;
import com.sxxm.stockknock.entity.NewsAnalysis;
import com.sxxm.stockknock.entity.Stock;
import com.sxxm.stockknock.repository.NewsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NewsService {

    @Autowired
    private NewsRepository newsRepository;

    @Autowired
    private AIService aiService;

    public List<NewsDto> getRecentNews(int days) {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(days);
        
        List<News> newsList = newsRepository.findByPublishedAtBetweenOrderByPublishedAtDesc(startDate, endDate);
        return newsList.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    public NewsDto getNewsById(Long newsId) {
        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new RuntimeException("뉴스를 찾을 수 없습니다."));
        return convertToDto(news);
    }

    public NewsAnalysisDto analyzeNews(Long newsId) {
        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new RuntimeException("뉴스를 찾을 수 없습니다."));

        // 이미 분석이 있으면 반환
        if (!news.getAnalyses().isEmpty()) {
            NewsAnalysis existingAnalysis = news.getAnalyses().get(0);
            return NewsAnalysisDto.builder()
                    .summary(existingAnalysis.getSummary())
                    .impactAnalysis(existingAnalysis.getImpactAnalysis())
                    .sentiment(existingAnalysis.getSentiment().toString())
                    .impactScore(existingAnalysis.getImpactScore())
                    .build();
        }

        // AI 분석 수행
        String analysisResult = aiService.analyzeNews(news.getContent());
        
        // 분석 결과를 파싱하고 저장하는 로직 (간단한 예시)
        NewsAnalysis analysis = NewsAnalysis.builder()
                .news(news)
                .summary(extractSummary(analysisResult))
                .impactAnalysis(analysisResult)
                .sentiment(determineSentiment(analysisResult))
                .impactScore(calculateImpactScore(analysisResult))
                .build();

        news.getAnalyses().add(analysis);
        newsRepository.save(news);

        return NewsAnalysisDto.builder()
                .summary(analysis.getSummary())
                .impactAnalysis(analysis.getImpactAnalysis())
                .sentiment(analysis.getSentiment().toString())
                .impactScore(analysis.getImpactScore())
                .build();
    }

    private String extractSummary(String analysis) {
        // 간단한 추출 로직 (실제로는 더 정교하게 파싱)
        return analysis.length() > 200 ? analysis.substring(0, 200) + "..." : analysis;
    }

    private NewsAnalysis.Sentiment determineSentiment(String analysis) {
        String lower = analysis.toLowerCase();
        if (lower.contains("긍정") || lower.contains("상승") || lower.contains("호재")) {
            return NewsAnalysis.Sentiment.POSITIVE;
        } else if (lower.contains("부정") || lower.contains("하락") || lower.contains("악재")) {
            return NewsAnalysis.Sentiment.NEGATIVE;
        }
        return NewsAnalysis.Sentiment.NEUTRAL;
    }

    private Integer calculateImpactScore(String analysis) {
        // 간단한 점수 계산 (실제로는 더 정교하게)
        return 5; // 기본값
    }

    public NewsDto convertToDto(News news) {
        List<String> relatedSymbols = news.getRelatedStocks() != null ?
                news.getRelatedStocks().stream().map(Stock::getSymbol).collect(Collectors.toList()) :
                List.of();

        NewsAnalysisDto analysisDto = null;
        if (!news.getAnalyses().isEmpty()) {
            NewsAnalysis analysis = news.getAnalyses().get(0);
            analysisDto = NewsAnalysisDto.builder()
                    .summary(analysis.getSummary())
                    .impactAnalysis(analysis.getImpactAnalysis())
                    .sentiment(analysis.getSentiment().toString())
                    .impactScore(analysis.getImpactScore())
                    .build();
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
}

