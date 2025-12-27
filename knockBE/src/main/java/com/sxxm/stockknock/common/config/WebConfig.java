package com.sxxm.stockknock.common.config;

/**
 * Web 설정
 * 
 * 역할:
 * - CORS 설정 (Cross-Origin Resource Sharing)
 * - 프론트엔드 도메인 허용 (localhost:3000, localhost:5173)
 * - HTTP 메서드 및 헤더 허용 설정
 */
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:5173", "http://localhost:3000")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}

