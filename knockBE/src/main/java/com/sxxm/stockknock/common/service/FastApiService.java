package com.sxxm.stockknock.common.service;

/**
 * FastAPI 서비스 클라이언트
 * 
 * 역할:
 * - Python FastAPI 서버와의 통신 담당
 * - WebClient를 사용한 비동기 HTTP 요청
 * - 주가 조회, AI 채팅, 뉴스 분석, 포트폴리오 분석 API 호출
 * - 타임아웃 및 에러 처리
 */
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
                            // HTTP 오류는 onErrorReturn에서 처리
                            return response.bodyToMono(String.class)
                                    .flatMap(body -> Mono.error(new RuntimeException("FastAPI 오류: " + response.statusCode())));
                        })
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(10))
                .map(response -> {
                    Object price = response.get("price");
                    if (price == null) {
                        return null;  // null 반환
                    }
                    if (price instanceof Number) {
                        return BigDecimal.valueOf(((Number) price).doubleValue());
                    } else if (price instanceof String) {
                        return new BigDecimal((String) price);
                    }
                    return null;  // null 반환 (BigDecimal.ZERO 대신)
                })
                .doOnError(error -> {
                    // 에러 로그는 상위에서 처리
                })
                .onErrorReturn(null);  // null 반환 (BigDecimal.ZERO 대신)
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
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> {
                            System.err.println("FastAPI 포트폴리오 분석 HTTP 오류: " + response.statusCode());
                            return response.bodyToMono(String.class)
                                    .flatMap(body -> {
                                        System.err.println("FastAPI 오류 응답 본문: " + body);
                                        return Mono.error(new RuntimeException("FastAPI 오류: " + response.statusCode()));
                                    });
                        })
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(10))
                .cast(Map.class)
                .map(m -> (Map<String, Object>) m)
                .doOnError(error -> {
                    System.err.println("FastAPI 포트폴리오 분석 예외: " + error.getMessage());
                    error.printStackTrace();
                })
                .onErrorReturn(java.util.Map.of("analysis", "포트폴리오 분석 중 오류가 발생했습니다."));
    }
}



