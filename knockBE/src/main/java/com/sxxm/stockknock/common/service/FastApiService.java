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
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> {
                            System.err.println("FastAPI 가격 조회 HTTP 오류: " + symbol + " - " + response.statusCode());
                            return response.bodyToMono(String.class)
                                    .flatMap(body -> {
                                        System.err.println("FastAPI 오류 응답 본문: " + body);
                                        return Mono.error(new RuntimeException("FastAPI 오류: " + response.statusCode() + " - " + body));
                                    });
                        })
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(10))
                .map(response -> {
                    Object price = response.get("price");
                    if (price instanceof Number) {
                        BigDecimal result = BigDecimal.valueOf(((Number) price).doubleValue());
                        System.out.println("FastAPI 가격 파싱 성공: " + symbol + " = " + result);
                        return result;
                    } else if (price instanceof String) {
                        BigDecimal result = new BigDecimal((String) price);
                        System.out.println("FastAPI 가격 파싱 성공 (문자열): " + symbol + " = " + result);
                        return result;
                    }
                    System.err.println("FastAPI 응답에 price 필드가 없거나 형식이 잘못됨: " + symbol + " - " + response);
                    return BigDecimal.ZERO;
                })
                .doOnError(error -> {
                    System.err.println("FastAPI 가격 조회 예외: " + symbol + " - " + error.getMessage());
                    error.printStackTrace();
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



