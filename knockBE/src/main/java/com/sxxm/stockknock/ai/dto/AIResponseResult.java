package com.sxxm.stockknock.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * GPT API 호출 결과 DTO
 * 
 * 역할:
 * - GPT API 호출 결과를 담는 객체
 * - 성공/실패 여부와 원인을 명확히 구분
 * - 실패 타입별 상세 정보 제공 (TIMEOUT, SOCKET_TIMEOUT, IO_ERROR 등)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIResponseResult {
    /**
     * 성공 여부
     */
    private boolean success;
    
    /**
     * GPT 응답 내용 (성공 시)
     */
    private String content;
    
    /**
     * 실패 원인 타입
     */
    private FailureType failureType;
    
    /**
     * 실패 메시지
     */
    private String errorMessage;
    
    /**
     * 실패 원인 타입
     */
    public enum FailureType {
        TIMEOUT,           // 타임아웃 (90초 초과)
        SOCKET_TIMEOUT,   // SocketTimeoutException
        IO_ERROR,         // IOException
        API_ERROR,        // OpenAI API 오류
        INITIALIZATION_ERROR, // OpenAiService 초기화 실패
        UNKNOWN_ERROR     // 기타 예외
    }
    
    /**
     * 성공 결과 생성
     */
    public static AIResponseResult success(String content) {
        return AIResponseResult.builder()
                .success(true)
                .content(content)
                .build();
    }
    
    /**
     * 실패 결과 생성
     */
    public static AIResponseResult failure(FailureType failureType, String errorMessage) {
        return AIResponseResult.builder()
                .success(false)
                .failureType(failureType)
                .errorMessage(errorMessage)
                .build();
    }
}

