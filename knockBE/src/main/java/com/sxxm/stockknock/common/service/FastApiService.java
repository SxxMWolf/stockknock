package com.sxxm.stockknock.common.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;

@Service
public class FastApiService {

    @Autowired
    private WebClient fastApiClient;

    /**
     * FastAPI에서 현재 주가 조회
     */
    public Mono<BigDecimal> getCurrentPrice(String symbol) {
        return fastApiClient.get()
                .uri("/api/stock/{symbol}/price", symbol)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(10))
                .map(response -> {
                    Object price = response.get("price");
                    if (price instanceof Number) {
                        return BigDecimal.valueOf(((Number) price).doubleValue());
                    } else if (price instanceof String) {
                        return new BigDecimal((String) price);
                    }
                    return BigDecimal.ZERO;
                })
                .onErrorReturn(BigDecimal.ZERO);
    }

    /**
     * FastAPI에서 주가 업데이트 요청
     */
    public Mono<Void> updateStockPrice(String symbol) {
        return fastApiClient.post()
                .uri("/api/stock/{symbol}/update", symbol)
                .retrieve()
                .bodyToMono(Void.class)
                .timeout(Duration.ofSeconds(30))
                .onErrorComplete();
    }

    /**
     * FastAPI에서 AI 채팅
     */
    public Mono<String> chatWithAI(String question, Long userId, String conversationHistory) {
        Map<String, Object> request = Map.of(
                "question", question,
                "user_id", userId,
                "conversation_history", conversationHistory != null ? conversationHistory : ""
        );

        return fastApiClient.post()
                .uri("/api/ai/chat")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(30))
                .map(response -> (String) response.get("response"))
                .onErrorReturn("AI 서비스 오류가 발생했습니다.");
    }

    /**
     * FastAPI에서 뉴스 분석
     */
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> analyzeNews(Long newsId) {
        return fastApiClient.post()
                .uri("/api/news/analyze/{newsId}", newsId)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(30))
                .cast(Map.class)
                .map(m -> (Map<String, Object>) m)
                .onErrorReturn(java.util.Collections.emptyMap());
    }

    /**
     * FastAPI에서 포트폴리오 AI 분석
     */
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> analyzePortfolio(Map<String, Object> request) {
        return fastApiClient.post()
                .uri("/api/ai/analyze-portfolio")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(30))
                .cast(Map.class)
                .map(m -> (Map<String, Object>) m)
                .onErrorReturn(java.util.Map.of("analysis", "포트폴리오 분석 중 오류가 발생했습니다."));
    }
}



