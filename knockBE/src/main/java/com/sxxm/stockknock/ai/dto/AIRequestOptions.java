package com.sxxm.stockknock.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * GPT API 호출 옵션 DTO
 * 
 * 역할:
 * - GPT API 호출 시 사용할 옵션 설정
 * - System Prompt, Temperature, Max Tokens, Timeout 설정
 * - 대화 기록(Conversation History) 포함
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIRequestOptions {
    /**
     * System prompt (기본값: 일반적인 금융 분석가)
     */
    @Builder.Default
    private String systemPrompt = "당신은 개인 투자자를 돕는 전문 금융 분석가입니다. " +
            "구체적이고 실용적인 투자 조언을 제공하며, 막연한 표현을 피하고 " +
            "실제 행동 가능한 구체적인 제안을 해주세요. " +
            "한국 개인 투자자 관점에서 이해하기 쉽게 설명해주세요.";
    
    /**
     * Temperature (기본값: 0.7)
     */
    @Builder.Default
    private double temperature = 0.7;
    
    /**
     * Max tokens (기본값: 2000)
     */
    @Builder.Default
    private int maxTokens = 2000;
    
    /**
     * Timeout in seconds (기본값: 90)
     */
    @Builder.Default
    private int timeoutSeconds = 90;
    
    /**
     * 대화 기록 (선택적)
     */
    private java.util.List<com.theokanning.openai.completion.chat.ChatMessage> conversationHistory;
}

