package com.sxxm.stockknock.news.service;

import com.sxxm.stockknock.news.entity.News;
import com.sxxm.stockknock.stock.entity.Stock;
import com.sxxm.stockknock.news.repository.NewsRepository;
import com.sxxm.stockknock.stock.repository.StockRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 뉴스 수집 서비스 (NewsAPI, RSS 피드 등)
 */
@Service
public class NewsCrawlerService {

    @Autowired
    private NewsRepository newsRepository;

    @Autowired
    private StockRepository stockRepository;

    @Value("${news.api.newsapi.key:}")
    private String newsApiKey;

    @Value("${news.api.enabled:true}")
    private boolean newsApiEnabled;

    private final WebClient webClient = WebClient.builder().build();

    @PostConstruct
    public void init() {
        // 환경 변수 및 시스템 프로퍼티 직접 확인 (디버깅용)
        String envKey = System.getenv("NEWS_API_KEY");
        String sysPropKey = System.getProperty("NEWS_API_KEY"); // VM options에서 전달
        String sysPropKey2 = System.getProperty("news.api.newsapi.key");
        
        System.out.println("[뉴스 수집][초기화] 환경 변수 NEWS_API_KEY: " + (envKey != null ? "설정됨 (길이: " + envKey.length() + ")" : "미설정"));
        System.out.println("[뉴스 수집][초기화] 시스템 프로퍼티 NEWS_API_KEY: " + (sysPropKey != null ? "설정됨 (길이: " + sysPropKey.length() + ")" : "미설정"));
        System.out.println("[뉴스 수집][초기화] 시스템 프로퍼티 news.api.newsapi.key: " + (sysPropKey2 != null ? "설정됨 (길이: " + sysPropKey2.length() + ")" : "미설정"));
        System.out.println("[뉴스 수집][초기화] @Value로 주입된 키: " + (newsApiKey != null && !newsApiKey.isEmpty() ? "설정됨 (길이: " + newsApiKey.length() + ")" : "미설정"));
        
        // 우선순위: 시스템 프로퍼티 > 환경 변수 > @Value
        String actualKey = null;
        if (sysPropKey != null && !sysPropKey.isEmpty()) {
            actualKey = sysPropKey;
            System.out.println("[뉴스 수집][초기화] 시스템 프로퍼티에서 설정합니다.");
        } else if (envKey != null && !envKey.isEmpty()) {
            actualKey = envKey;
            System.out.println("[뉴스 수집][초기화] 환경 변수에서 설정합니다.");
        } else if (sysPropKey2 != null && !sysPropKey2.isEmpty()) {
            actualKey = sysPropKey2;
            System.out.println("[뉴스 수집][초기화] 시스템 프로퍼티(2)에서 설정합니다.");
        }
        
        if (actualKey != null && (newsApiKey == null || newsApiKey.isEmpty())) {
            newsApiKey = actualKey;
        }
    }

    /**
     * NewsAPI를 통해 주식 관련 뉴스 수집
     * 무료 API, 제한: 일일 100회 요청
     * 
     * @param query 검색 쿼리
     * @return 수집된 뉴스 개수 (0이면 fallback 필요)
     */
    private int collectNewsFromNewsAPI(String query) {
        // 환경 변수 직접 확인 (IntelliJ IDEA에서 환경 변수가 전달되지 않는 경우 대비)
        String actualKey = newsApiKey;
        if (actualKey == null || actualKey.isEmpty()) {
            actualKey = System.getenv("NEWS_API_KEY");
            if (actualKey != null && !actualKey.isEmpty()) {
                System.out.println("[뉴스 수집] 환경 변수에서 직접 읽어옵니다.");
                newsApiKey = actualKey; // 다음 호출을 위해 저장
            }
        }
        
        System.out.println("[뉴스 수집] NewsAPI 키 확인: " + (actualKey != null && !actualKey.isEmpty() ? "설정됨 (길이: " + actualKey.length() + ", 앞 4자리: " + (actualKey.length() >= 4 ? actualKey.substring(0, 4) : "N/A") + "...)" : "미설정"));
        
        if (actualKey == null || actualKey.isEmpty()) {
            System.err.println("[뉴스 수집] 경고: NewsAPI 키가 설정되지 않았습니다.");
            System.err.println("[뉴스 수집] 해결 방법:");
            System.err.println("[뉴스 수집] 1. https://newsapi.org/register 에서 API 키 발급");
            System.err.println("[뉴스 수집] 2. 환경 변수 설정: export NEWS_API_KEY=your_api_key_here");
            System.err.println("[뉴스 수집] 3. IDE 실행 설정에서 환경 변수 추가 (IntelliJ: Run > Edit Configurations > Environment variables)");
            System.err.println("[뉴스 수집] 4. 애플리케이션 재시작");
            return 0;
        }

        try {
            System.out.println("[뉴스 수집] NewsAPI 호출 시작 - 쿼리: " + query);
            // URL 인코딩 처리
            String encodedQuery = java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8);
            
            // 최근 3일 이내 뉴스 조회
            LocalDate threeDaysAgo = LocalDate.now().minusDays(3);
            String fromDate = threeDaysAgo.atStartOfDay().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));
            System.out.println("[뉴스 수집] from 날짜: " + fromDate);
            
            // 영어(en)와 한글(ko) 뉴스만 수집
            String url = String.format(
                    "https://newsapi.org/v2/everything?q=%s&sortBy=publishedAt&from=%s&language=en,ko&apiKey=%s",
                    encodedQuery, fromDate, actualKey
            );
            System.out.println("[뉴스 수집] NewsAPI URL 전체: " + url.replace(actualKey, "***"));

            System.out.println("[뉴스 수집] NewsAPI 호출 시작...");
            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(java.time.Duration.ofSeconds(30))
                    .block();

            System.out.println("[뉴스 수집] NewsAPI 응답 받음. 응답이 null인가? " + (response == null));
            
            if (response != null) {
                System.out.println("[뉴스 수집] 응답 키 목록: " + response.keySet());
                
                // NewsAPI 에러 응답 체크
                if (response.containsKey("status")) {
                    String status = (String) response.get("status");
                    System.out.println("[뉴스 수집] NewsAPI status: " + status);
                    
                    if (!"ok".equals(status)) {
                        String errorMessage = (String) response.getOrDefault("message", "알 수 없는 오류");
                        System.err.println("[뉴스 수집] NewsAPI 오류: " + errorMessage);
                        System.err.println("[뉴스 수집] 전체 응답: " + response);
                        return 0;
                    }
                }
                
                if (response.containsKey("articles")) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> articles = (List<Map<String, Object>>) response.get("articles");
                    Integer totalResults = (Integer) response.getOrDefault("totalResults", 0);
                    System.out.println("[뉴스 수집] NewsAPI totalResults: " + totalResults);
                    System.out.println("[뉴스 수집] NewsAPI에서 받은 뉴스 개수: " + (articles != null ? articles.size() : 0));

                    if (articles == null || articles.isEmpty()) {
                        System.out.println("[뉴스 수집] NewsAPI에서 받은 뉴스가 없습니다. (수집된 기사 개수: 0)");
                        return 0;
                    }
                    
                    System.out.println("[뉴스 수집] 수집된 기사 개수: " + articles.size());

                    int savedCount = 0;
                    int duplicateCount = 0;
                    int errorCount = 0;
                    
                    for (int i = 0; i < articles.size(); i++) {
                        try {
                            Map<String, Object> article = articles.get(i);
                            String title = (String) article.get("title");
                            
                            if (title == null || title.trim().isEmpty()) {
                                System.out.println("[뉴스 수집] 제목이 없는 뉴스 건너뜀: " + i);
                                continue;
                            }
                            
                            String content = (String) article.get("content");
                            @SuppressWarnings("unchecked")
                            Map<String, Object> sourceMap = (Map<String, Object>) article.get("source");
                            String source = sourceMap != null ? (String) sourceMap.get("name") : "Unknown";
                            String articleUrl = (String) article.get("url");
                            String publishedAtStr = (String) article.get("publishedAt");

                            // 언어 필터링: 영어와 한글만 허용
                            String textToCheck = (title + " " + (content != null ? content : "")).trim();
                            if (!isEnglishOrKorean(textToCheck)) {
                                System.out.println("[뉴스 수집] 영어/한글이 아닌 뉴스 제외: " + title.substring(0, Math.min(50, title.length())));
                                continue;
                            }

                            // 중복 체크
                            if (newsRepository.findByTitleContainingOrContentContaining(title, title).isEmpty()) {
                                News news = News.builder()
                                        .title(title)
                                        .content(content != null ? content : "")
                                        .source(source != null ? source : "Unknown")
                                        .url(articleUrl != null ? articleUrl : "")
                                        .publishedAt(parseDateTime(publishedAtStr))
                                        .build();

                                newsRepository.save(news);
                                savedCount++;
                                
                                if (savedCount % 10 == 0) {
                                    System.out.println("[뉴스 수집] 진행 중... 저장된 뉴스: " + savedCount);
                                }
                            } else {
                                duplicateCount++;
                            }
                        } catch (Exception e) {
                            errorCount++;
                            System.err.println("[뉴스 수집] 뉴스 처리 오류 (인덱스 " + i + "): " + e.getMessage());
                            if (errorCount > 5) {
                                System.err.println("[뉴스 수집] 오류가 너무 많아 중단합니다.");
                                break;
                            }
                        }
                    }
                    System.out.println("[뉴스 수집] ===== 수집 완료 =====");
                    System.out.println("[뉴스 수집] DB에 저장된 새 뉴스 개수: " + savedCount);
                    System.out.println("[뉴스 수집] 중복으로 제외된 뉴스 개수: " + duplicateCount);
                    System.out.println("[뉴스 수집] 처리 오류 발생한 뉴스 개수: " + errorCount);
                    return savedCount;
                } else {
                    System.err.println("[뉴스 수집] NewsAPI 응답에 'articles' 키가 없습니다.");
                    System.err.println("[뉴스 수집] 응답 내용: " + response);
                    return 0;
                }
            } else {
                System.err.println("[뉴스 수집] NewsAPI 응답이 null입니다.");
                return 0;
            }
        } catch (Exception e) {
            System.err.println("[뉴스 수집] NewsAPI 수집 오류: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 주식 심볼별 뉴스 수집
     */
    public void collectNewsForStock(String stockSymbol) {
        Stock stock = stockRepository.findBySymbol(stockSymbol).orElse(null);
        if (stock == null) {
            return;
        }

        // 종목명과 심볼로 검색: ("{종목명} Inc" OR {심볼})
        String query = String.format("(\"%s Inc\" OR %s)", stock.getName(), stockSymbol);
        collectNewsFromNewsAPI(query);
    }

    /**
     * 일반 주식 뉴스 수집
     * 주식 관련 핵심 키워드만 필터링하여 수집
     */
    public void collectGeneralStockNews() {
        // 주식 관련 키워드 (간단하고 효과적인 쿼리)
        // "stock market"을 기본으로 사용하고, 결과가 없으면 더 넓은 범위로 확장
        String initialQuery = "stock market";
        int collectedCount = collectNewsFromNewsAPI(initialQuery);
        
        // 결과가 0건일 경우 더 넓은 범위의 쿼리로 재시도
        if (collectedCount == 0) {
            System.out.println("[뉴스 수집] 초기 쿼리 결과가 0건이므로 확장 쿼리로 재시도합니다.");
            String expandedQuery = "stock AND (market OR price OR trading OR exchange)";
            int expandedCount = collectNewsFromNewsAPI(expandedQuery);
            if (expandedCount > 0) {
                System.out.println("[뉴스 수집] 확장 쿼리로 " + expandedCount + "건 수집 완료");
            } else {
                // 마지막 fallback: 더 넓은 범위
                System.out.println("[뉴스 수집] 확장 쿼리도 0건이므로 fallback 쿼리로 재시도합니다.");
                String fallbackQuery = "stock";
                int fallbackCount = collectNewsFromNewsAPI(fallbackQuery);
                if (fallbackCount > 0) {
                    System.out.println("[뉴스 수집][Fallback 사용] fallback 쿼리로 " + fallbackCount + "건 수집 완료");
                }
            }
        }
    }

    /**
     * 날짜 문자열 파싱
     */
    private LocalDateTime parseDateTime(String dateTimeStr) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
            return LocalDateTime.parse(dateTimeStr, formatter);
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }

    /**
     * 텍스트가 영어 또는 한글로만 구성되어 있는지 확인
     * 다른 언어(중국어, 일본어, 아랍어 등)가 주로 포함되어 있으면 false 반환
     * 
     * @param text 확인할 텍스트
     * @return 영어 또는 한글이 주로 포함되어 있으면 true
     */
    private boolean isEnglishOrKorean(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }

        // 전체 텍스트에서 허용된 문자 비율 계산
        int totalChars = 0;
        int allowedChars = 0;
        
        for (char c : text.toCharArray()) {
            totalChars++;
            // 한글, 영어, 숫자, 공백, 기본 구두점인지 확인
            if (Character.isLetterOrDigit(c) || 
                (c >= '가' && c <= '힣') || 
                Character.isWhitespace(c) ||
                ".,!?;:\"'()[]{}-_+=@#$%^&*<>/\\|`~".indexOf(c) >= 0) {
                allowedChars++;
            }
        }
        
        // 허용된 문자가 전체의 80% 이상이면 영어/한글 뉴스로 간주
        if (totalChars == 0) {
            return false;
        }
        
        double allowedRatio = (double) allowedChars / totalChars;
        return allowedRatio >= 0.8;
    }

}

