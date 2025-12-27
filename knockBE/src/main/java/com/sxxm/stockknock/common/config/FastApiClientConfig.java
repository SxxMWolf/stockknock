package com.sxxm.stockknock.common.config;

/**
 * FastAPI 클라이언트 설정
 * 
 * 역할:
 * - WebClient Bean 생성 (FastAPI 통신용)
 * - FastAPI 서버 URL 설정
 * - 비동기 HTTP 클라이언트 구성
 */
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class FastApiClientConfig {

    @Value("${fastapi.base-url:http://localhost:8000}")
    private String fastApiBaseUrl;

    @Bean
    public WebClient fastApiClient() {
        return WebClient.builder()
                .baseUrl(fastApiBaseUrl)
                .build();
    }
}















