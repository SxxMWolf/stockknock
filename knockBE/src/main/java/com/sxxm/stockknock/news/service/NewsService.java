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
import java.util.*;
import java.util.stream.Collectors;

@Service
public class NewsService {

    @Autowired
    private NewsRepository newsRepository;

    @Autowired
    private FastApiService fastApiService;

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
                NewsAnalysis analysis = NewsAnalysis.builder()
                        .newsId(news.getId())
                        .news(news)
                        .summary((String) analysisResult.getOrDefault("summary", ""))
                        .aiComment((String) analysisResult.getOrDefault("ai_comment", ""))
                        .sentiment((String) analysisResult.getOrDefault("sentiment", "neutral"))
                        .impactScore(((Number) analysisResult.getOrDefault("impact_score", 5)).intValue())
                        .build();

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

    private String extractSummary(String analysis) {
        // 간단한 추출 로직 (실제로는 더 정교하게 파싱)
        return analysis.length() > 200 ? analysis.substring(0, 200) + "..." : analysis;
    }

    private String determineSentiment(String analysis) {
        String lower = analysis.toLowerCase();
        if (lower.contains("긍정") || lower.contains("상승") || lower.contains("호재")) {
            return "positive";
        } else if (lower.contains("부정") || lower.contains("하락") || lower.contains("악재")) {
            return "negative";
        }
        return "neutral";
    }

    private Integer calculateImpactScore(String analysis) {
        // 간단한 점수 계산 (실제로는 더 정교하게)
        return 5; // 기본값
    }

    public NewsDto convertToDto(News news) {
        List<String> relatedSymbols = news.getStockRelations() != null ?
                news.getStockRelations().stream()
                        .map(rel -> rel.getStockSymbol())
                        .collect(Collectors.toList()) :
                List.of();

        NewsAnalysisDto analysisDto = null;
        if (news.getAnalysis() != null) {
            NewsAnalysis analysis = news.getAnalysis();
            analysisDto = NewsAnalysisDto.builder()
                    .summary(analysis.getSummary())
                    .impactAnalysis(analysis.getAiComment())
                    .sentiment(analysis.getSentiment())
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

    /**
     * 주요 뉴스 자동 분석 (비용 최적화: 상위 20개만 분석)
     * 골든 뉴스(주가 영향 높은 뉴스) 우선 분석
     */
    public void analyzeTopNews(int limit) {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusHours(24); // 최근 24시간 내 뉴스

        // 미분석 뉴스 조회
        List<News> unanalyzedNews = newsRepository
                .findByPublishedAtBetweenOrderByPublishedAtDesc(startDate, endDate)
                .stream()
                .filter(news -> news.getAnalysis() == null)
                .collect(Collectors.toList());

        // 골든 뉴스 키워드 필터링 및 우선순위 부여
        List<News> prioritizedNews = prioritizeGoldenNews(unanalyzedNews);

        // 중복 제거
        List<News> deduplicatedNews = removeDuplicateNews(prioritizedNews);

        // 상위 N개만 선택
        List<News> topNews = deduplicatedNews.stream()
                .limit(limit)
                .collect(Collectors.toList());

        System.out.println("주요 뉴스 분석 시작: " + topNews.size() + "개");

        // 선택된 뉴스 분석
        for (News news : topNews) {
            try {
                analyzeNews(news.getId());
                System.out.println("뉴스 분석 완료: " + news.getTitle());
            } catch (Exception e) {
                System.err.println("뉴스 분석 실패: " + news.getTitle() + " - " + e.getMessage());
            }
        }

        System.out.println("주요 뉴스 분석 완료");
    }

    /**
     * 골든 뉴스 우선순위 부여
     * 주가에 큰 영향을 미칠 수 있는 키워드가 포함된 뉴스를 우선순위로 설정
     */
    private List<News> prioritizeGoldenNews(List<News> newsList) {
        // 골든 뉴스 키워드 (주가 영향 높은 키워드)
        Set<String> goldenKeywords = Set.of(
                "실적", "실적 발표", "분기 실적", "연간 실적",
                "인수합병", "M&A", "합병", "인수",
                "배당", "배당금", "배당률",
                "주가 급등", "주가 급락", "급등", "급락",
                "실적 부진", "실적 호조", "실적 개선",
                "신제품 출시", "신규 사업", "사업 확장",
                "규제", "규제 완화", "규제 강화",
                "경영진", "CEO", "임원 교체",
                "주식 분할", "액면 분할", "무상증자",
                "공시", "공시사항", "중요 공시"
        );

        return newsList.stream()
                .map(news -> {
                    // 골든 뉴스 점수 계산
                    int score = calculateGoldenNewsScore(news, goldenKeywords);
                    return new AbstractMap.SimpleEntry<>(news, score);
                })
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue())) // 점수 높은 순 정렬
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * 골든 뉴스 점수 계산
     */
    private int calculateGoldenNewsScore(News news, Set<String> goldenKeywords) {
        int score = 0;
        String title = news.getTitle() != null ? news.getTitle().toLowerCase() : "";
        String content = news.getContent() != null ? news.getContent().toLowerCase() : "";

        for (String keyword : goldenKeywords) {
            String lowerKeyword = keyword.toLowerCase();
            if (title.contains(lowerKeyword)) {
                score += 3; // 제목에 포함되면 높은 점수
            } else if (content.contains(lowerKeyword)) {
                score += 1; // 본문에 포함되면 낮은 점수
            }
        }

        // 주요 종목 관련 뉴스도 가산점
        String[] majorStocks = {"삼성전자", "SK하이닉스", "네이버", "카카오", "LG전자",
                "AAPL", "MSFT", "GOOGL", "TSLA", "AMZN"};
        for (String stock : majorStocks) {
            if (title.contains(stock) || content.contains(stock)) {
                score += 2;
            }
        }

        return score;
    }

    /**
     * 중복 뉴스 제거 (제목 유사도 기반)
     */
    private List<News> removeDuplicateNews(List<News> newsList) {
        List<News> deduplicated = new ArrayList<>();
        Set<String> seenTitles = new HashSet<>();

        for (News news : newsList) {
            String normalizedTitle = normalizeTitle(news.getTitle());
            
            // 이미 본 제목과 유사한지 체크
            boolean isDuplicate = false;
            for (String seenTitle : seenTitles) {
                if (calculateSimilarity(normalizedTitle, seenTitle) > 0.7) {
                    isDuplicate = true;
                    break;
                }
            }

            if (!isDuplicate) {
                deduplicated.add(news);
                seenTitles.add(normalizedTitle);
            }
        }

        return deduplicated;
    }

    /**
     * 제목 정규화 (공백 제거, 소문자 변환)
     */
    private String normalizeTitle(String title) {
        if (title == null) return "";
        return title.toLowerCase().replaceAll("\\s+", "").trim();
    }

    /**
     * 간단한 문자열 유사도 계산 (Jaccard 유사도 기반)
     */
    private double calculateSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null || s1.isEmpty() || s2.isEmpty()) {
            return 0.0;
        }

        Set<Character> set1 = new HashSet<>();
        Set<Character> set2 = new HashSet<>();

        for (char c : s1.toCharArray()) {
            set1.add(c);
        }
        for (char c : s2.toCharArray()) {
            set2.add(c);
        }

        Set<Character> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        Set<Character> union = new HashSet<>(set1);
        union.addAll(set2);

        if (union.isEmpty()) return 0.0;

        return (double) intersection.size() / union.size();
    }
}

