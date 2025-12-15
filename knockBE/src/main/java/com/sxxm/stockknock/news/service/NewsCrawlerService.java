package com.sxxm.stockknock.news.service;

import com.sxxm.stockknock.news.entity.News;
import com.sxxm.stockknock.stock.entity.Stock;
import com.sxxm.stockknock.news.repository.NewsRepository;
import com.sxxm.stockknock.stock.repository.StockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

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

    /**
     * NewsAPI를 통해 주식 관련 뉴스 수집
     * 무료 API, 제한: 일일 100회 요청
     */
    public void collectNewsFromNewsAPI(String query) {
        if (newsApiKey == null || newsApiKey.isEmpty()) {
            return;
        }

        try {
            String url = String.format(
                    "https://newsapi.org/v2/everything?q=%s&language=ko&sortBy=publishedAt&apiKey=%s",
                    query, newsApiKey
            );

            Map<String, Object> response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(java.time.Duration.ofSeconds(10))
                    .block();

            if (response != null && response.containsKey("articles")) {
                List<Map<String, Object>> articles = (List<Map<String, Object>>) response.get("articles");

                for (Map<String, Object> article : articles) {
                    String title = (String) article.get("title");
                    String content = (String) article.get("content");
                    String source = (String) ((Map<String, Object>) article.get("source")).get("name");
                    String articleUrl = (String) article.get("url");
                    String publishedAtStr = (String) article.get("publishedAt");

                    // 중복 체크
                    if (newsRepository.findByTitleContainingOrContentContaining(title, title).isEmpty()) {
                        News news = News.builder()
                                .title(title)
                                .content(content != null ? content : "")
                                .source(source)
                                .url(articleUrl)
                                .publishedAt(parseDateTime(publishedAtStr))
                                .build();

                        newsRepository.save(news);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("NewsAPI 수집 오류: " + e.getMessage());
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

        // 종목명과 심볼로 검색
        collectNewsFromNewsAPI(stock.getName() + " OR " + stockSymbol);
    }

    /**
     * 일반 주식 뉴스 수집
     */
    public void collectGeneralStockNews() {
        collectNewsFromNewsAPI("주식 OR 증시 OR 투자");
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
}

