/**
 * 하위 호환성을 위한 래퍼 클래스. GPTClientService, AIChatService로 위임.
 * @deprecated 새 코드는 GPTClientService 또는 AIChatService를 직접 사용하세요.
 */
package com.sxxm.stockknock.ai.service;

import com.sxxm.stockknock.ai.dto.AIResponseResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Deprecated
public class AIService {

    private static final Logger log = LoggerFactory.getLogger(AIService.class);

    @Autowired
    private GPTClientService gptClientService;

    @Autowired
    private AIChatService aiChatService;

    /**
     * AI 주식 분석가 - 대화형 질문 응답 (비동기)
     * 
     * @deprecated AIChatService.answerQuestionWithContextAsync() 사용 권장
     */
    @Deprecated
    public Mono<AIResponseResult> answerQuestionWithContextAsync(
            String question,
            String conversationHistory,
            int historyCount) {
        log.warn("AIService.answerQuestionWithContextAsync()는 deprecated입니다. AIChatService를 사용하세요.");
        return aiChatService.answerQuestionWithContextAsync(question, conversationHistory, historyCount);
    }

    /**
     * GPT 비동기 응답 생성
     * 
     * @deprecated GPTClientService.generateResponseAsync() 사용 권장
     */
    @Deprecated
    public Mono<AIResponseResult> generateResponseAsync(String prompt) {
        log.warn("AIService.generateResponseAsync()는 deprecated입니다. GPTClientService를 사용하세요.");
        return gptClientService.generateResponseAsync(prompt);
    }
}
