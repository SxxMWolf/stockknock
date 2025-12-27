/**
 * AI 주식 분석가와의 대화형 질문 응답 처리. System Prompt 생성 및 대화 기록 관리.
 * GPTClientService를 사용하여 실제 GPT 호출 수행.
 */
package com.sxxm.stockknock.ai.service;

import com.sxxm.stockknock.ai.dto.AIRequestOptions;
import com.sxxm.stockknock.ai.dto.AIResponseResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Service
public class AIChatService {

    @Autowired
    private GPTClientService gptClientService;

    /**
     * AI 주식 분석가 - 대화형 질문 응답 (비동기)
     *
     * @param question 사용자 질문
     * @param conversationHistory 대화 기록 (문자열 형식)
     * @param historyCount 대화 기록 개수
     * @return AI 응답 결과
     */
    public Mono<AIResponseResult> answerQuestionWithContextAsync(
            String question,
            String conversationHistory,
            int historyCount) {

        String systemPrompt = buildSystemPrompt(historyCount);

        // 대화 기록을 ChatMessage 리스트로 변환
        List<ChatMessage> conversationMessages = parseConversationHistory(conversationHistory, historyCount);

        AIRequestOptions options = AIRequestOptions.builder()
                .systemPrompt(systemPrompt)
                .temperature(0.7)
                .maxTokens(1000)
                .conversationHistory(conversationMessages)
                .timeoutSeconds(90)
                .build();

        return gptClientService.generateResponseAsync(question, options);
    }

    /**
     * System Prompt 생성
     */
    private String buildSystemPrompt(int historyCount) {
        String systemPrompt = """
                너는 한국 주식 시장을 해석해주는 AI 주식 분석가다.

                중요한 전제:
                - 실시간 뉴스 기사나 데이터에 직접 접근하지는 않는다.
                - 그러나 오늘 날짜 기준으로 일반적으로 논의되는
                  시장 흐름, 수급, 투자자 심리를 분석가 관점에서 설명할 수 있다.

                행동 규칙:
                1. "제공할 수 없습니다", "알 수 없습니다" 같은 회피 표현을 사용하지 않는다.
                2. 질문에 '오늘', '현재', '뉴스'가 포함되어도
                   시장 해석과 흐름 중심으로 답변한다.
                3. 단정이 어려운 경우에도
                   "현재 시장에서는 보통 ○○ 흐름이 나타난다" 형태로 설명한다.
                4. 뉴스 브리핑처럼 요약하지 말고,
                   질문에 대한 분석형 설명으로 답한다.
                5. 한국 개인 투자자가 이해하기 쉬운 말투를 사용한다.
                6. 너는 정보 차단을 안내하는 챗봇이 아니라,
                   시장을 풀어서 설명해주는 분석가다.
                """;

        if (historyCount > 0) {
            systemPrompt += "\n이전 대화 맥락을 고려하되, 현재 질문에 집중해서 답변한다.";
        }

        return systemPrompt;
    }

    /**
     * 대화 기록 파싱
     */
    private List<ChatMessage> parseConversationHistory(String conversationHistory, int historyCount) {
        List<ChatMessage> conversationMessages = new ArrayList<>();

        if (historyCount > 0 && conversationHistory != null && !conversationHistory.isBlank()) {
            for (String line : conversationHistory.split("\n")) {
                if (line.startsWith("사용자: ")) {
                    conversationMessages.add(new ChatMessage(
                            ChatMessageRole.USER.value(),
                            line.substring(5)
                    ));
                } else if (line.startsWith("AI: ")) {
                    conversationMessages.add(new ChatMessage(
                            ChatMessageRole.ASSISTANT.value(),
                            line.substring(4)
                    ));
                }
            }
        }

        return conversationMessages;
    }
}

