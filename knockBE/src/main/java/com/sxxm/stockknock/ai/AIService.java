package com.sxxm.stockknock.ai;

import com.theokanning.openai.service.OpenAiService;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AIService {
    
    @Value("${openai.api.key}")
    private String apiKey;

    private OpenAiService getOpenAiService() {
        return new OpenAiService(apiKey);
    }

    public String analyzeNews(String newsContent) {
        String prompt = "다음 뉴스 기사를 분석하고 요약해주세요. 핵심 내용을 간단히 정리하고, 주가에 미칠 영향을 분석해주세요:\n\n" + newsContent;
        return generateResponse(prompt);
    }

    public String predictStock(String stockInfo, String historicalData) {
        String prompt = String.format(
            "다음 주식 정보와 과거 데이터를 바탕으로 주가 전망을 분석해주세요:\n\n" +
            "주식 정보:\n%s\n\n" +
            "과거 데이터:\n%s\n\n" +
            "단기 및 장기 관점에서 주가 흐름을 예측하고, 신뢰도를 백분율로 제시해주세요.",
            stockInfo, historicalData
        );
        return generateResponse(prompt);
    }

    public String generatePortfolioComment(String portfolioSummary) {
        String prompt = String.format(
            "다음 포트폴리오 정보를 바탕으로 분석 코멘트를 작성해주세요:\n\n%s\n\n" +
            "포트폴리오의 강점, 약점, 리밸런싱 제안을 포함해주세요.",
            portfolioSummary
        );
        return generateResponse(prompt);
    }

    public String answerQuestion(String question, String context) {
        String prompt = String.format(
            "다음 질문에 대해 주식 투자 관점에서 답변해주세요:\n\n" +
            "질문: %s\n\n" +
            "관련 정보: %s\n\n" +
            "명확하고 전문적인 답변을 제공해주세요.",
            question, context
        );
        return generateResponse(prompt);
    }

    private String generateResponse(String prompt) {
        try {
            OpenAiService service = getOpenAiService();
            
            List<ChatMessage> messages = new ArrayList<>();
            ChatMessage systemMessage = new ChatMessage(
                ChatMessageRole.SYSTEM.value(),
                "당신은 전문 주식 투자 분석가입니다. 정확하고 명확한 분석을 제공해주세요."
            );
            ChatMessage userMessage = new ChatMessage(
                ChatMessageRole.USER.value(),
                prompt
            );
            messages.add(systemMessage);
            messages.add(userMessage);

            ChatCompletionRequest completionRequest = ChatCompletionRequest.builder()
                    .model("gpt-4")
                    .messages(messages)
                    .temperature(0.7)
                    .maxTokens(1000)
                    .build();

            return service.createChatCompletion(completionRequest)
                    .getChoices()
                    .get(0)
                    .getMessage()
                    .getContent();
        } catch (Exception e) {
            e.printStackTrace();
            return "AI 분석 중 오류가 발생했습니다: " + e.getMessage();
        }
    }
}

