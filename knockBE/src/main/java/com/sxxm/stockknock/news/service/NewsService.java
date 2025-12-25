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

    /**
     * 최근 뉴스 조회 (DB에서만 조회)
     * 
     * 프론트엔드 요청 시 외부 API를 호출하지 않고,
     * 정기적으로 수집된 DB의 뉴스만 반환합니다.
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
                System.out.println("[뉴스 조회] 경고: 데이터베이스에 뉴스 데이터가 없습니다. 스케줄러가 뉴스를 수집할 때까지 기다려주세요.");
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

    @Autowired
    private com.sxxm.stockknock.ai.service.AIService aiService;

    /**
     * 오늘의 주요 뉴스 5줄 요약
     * 최근 24시간 내 뉴스 중 상위 10개를 선택하여 AI로 요약
     */
    public String summarizeTodayNews() {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusHours(24); // 최근 24시간

        // 최근 24시간 내 뉴스 조회
        List<News> todayNews = newsRepository
                .findByPublishedAtBetweenOrderByPublishedAtDesc(startDate, endDate);

        if (todayNews.isEmpty()) {
            return "오늘 수집된 뉴스가 없습니다.";
        }

        // 상위 10개 뉴스 선택 (이미 분석된 뉴스 우선)
        List<News> topNews = todayNews.stream()
                .sorted((a, b) -> {
                    // 분석된 뉴스 우선
                    boolean aHasAnalysis = a.getAnalysis() != null;
                    boolean bHasAnalysis = b.getAnalysis() != null;
                    if (aHasAnalysis != bHasAnalysis) {
                        return bHasAnalysis ? 1 : -1;
                    }
                    // 최신순
                    return b.getPublishedAt().compareTo(a.getPublishedAt());
                })
                .limit(10)
                .collect(Collectors.toList());

        // 뉴스 제목과 요약 수집
        StringBuilder newsText = new StringBuilder();
        newsText.append("다음은 오늘의 주요 주식 뉴스들입니다. 이 뉴스들을 종합하여 5줄로 요약해주세요:\n\n");
        
        for (int i = 0; i < topNews.size(); i++) {
            News news = topNews.get(i);
            newsText.append((i + 1)).append(". ").append(news.getTitle()).append("\n");
            
            // 분석된 요약이 있으면 사용, 없으면 제목만
            if (news.getAnalysis() != null && news.getAnalysis().getSummary() != null) {
                String summary = news.getAnalysis().getSummary();
                if (!summary.isEmpty() && !summary.equals("분석 중 오류가 발생했습니다.")) {
                    newsText.append("   ").append(summary).append("\n");
                }
            }
            newsText.append("\n");
        }

        // AIService를 통해 요약 요청
        try {
            String prompt = newsText.toString() + "\n위 뉴스들을 종합하여 5줄로 요약해주세요. 각 줄은 핵심 내용을 간결하게 담아주세요.";
            String summary = aiService.generateResponse(prompt);
            return summary;
        } catch (Exception e) {
            System.err.println("뉴스 요약 실패: " + e.getMessage());
            e.printStackTrace();
        }

        // AI 실패 시 간단한 요약 생성
        return generateSimpleSummary(topNews);
    }

    /**
     * 간단한 요약 생성 (AI 실패 시 사용)
     */
    private String generateSimpleSummary(List<News> newsList) {
        if (newsList.isEmpty()) {
            return "오늘 수집된 뉴스가 없습니다.";
        }

        StringBuilder summary = new StringBuilder();
        
        int count = Math.min(5, newsList.size());
        for (int i = 0; i < count; i++) {
            News news = newsList.get(i);
            summary.append((i + 1)).append(". ").append(news.getTitle());
            
            if (news.getAnalysis() != null && news.getAnalysis().getSummary() != null) {
                String analysisSummary = news.getAnalysis().getSummary();
                if (!analysisSummary.isEmpty() && !analysisSummary.equals("분석 중 오류가 발생했습니다.")) {
                    // 요약이 너무 길면 자르기
                    String shortSummary = analysisSummary.length() > 80 
                            ? analysisSummary.substring(0, 80) + "..." 
                            : analysisSummary;
                    summary.append(" - ").append(shortSummary);
                }
            }
            if (i < count - 1) {
                summary.append("\n");
            }
        }
        
        return summary.toString();
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

