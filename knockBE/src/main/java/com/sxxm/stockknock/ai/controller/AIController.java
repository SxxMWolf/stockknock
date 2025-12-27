/**
 * AI 주식 분석가와의 대화형 질문 응답 API. 최근 5개 대화 기록 기반 문맥 유지.
 */
package com.sxxm.stockknock.ai.controller;

import com.sxxm.stockknock.ai.dto.AIChatRequest;
import com.sxxm.stockknock.ai.dto.AIChatResponse;
import com.sxxm.stockknock.ai.entity.AIConversation;
import com.sxxm.stockknock.auth.entity.User;
import com.sxxm.stockknock.ai.repository.AIConversationRepository;
import com.sxxm.stockknock.auth.service.UserService;
import com.sxxm.stockknock.common.service.FastApiService;
import com.sxxm.stockknock.common.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "http://localhost:3000")
public class AIController {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserService userService;

    @Autowired
    private AIConversationRepository conversationRepository;

    @Autowired
    private FastApiService fastApiService;
    
    @Autowired
    private com.sxxm.stockknock.ai.service.AIChatService aiChatService;

    @PostMapping("/chat")
    public ResponseEntity<AIChatResponse> chat(
            @RequestHeader("Authorization") String token,
            @RequestBody AIChatRequest request) {
        try {
            Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
            User user = userService.getUserEntityById(userId);

            // 최근 대화 기록 가져오기 (문맥 유지)
            List<AIConversation> recentConversations = conversationRepository
                    .findByUserIdOrderByCreatedAtDesc(userId)
                    .stream()
                    .limit(5) // 최근 5개 대화만
                    .toList();

            // 대화 문맥 구성
            StringBuilder context = new StringBuilder();
            for (int i = recentConversations.size() - 1; i >= 0; i--) {
                AIConversation conv = recentConversations.get(i);
                context.append(conv.getRole()).append(": ").append(conv.getMessage()).append("\n");
            }

            // AI 응답 생성 (비동기 통일 API 사용)
            String response;
            try {
                com.sxxm.stockknock.ai.dto.AIResponseResult result = aiChatService.answerQuestionWithContextAsync(
                        request.getQuestion(),
                        context.toString(),
                        recentConversations.size()
                ).block(java.time.Duration.ofSeconds(90));
                
                if (result != null && result.isSuccess() && result.getContent() != null) {
                    response = result.getContent();
                } else {
                    // GPT API 실패 시 FastAPI로 폴백
                    String errorMsg = result != null ? result.getErrorMessage() : "알 수 없는 오류";
                    System.err.println("백엔드 GPT API 실패 (" + (result != null ? result.getFailureType() : "UNKNOWN") + "), FastAPI로 폴백: " + errorMsg);
                    response = fastApiService.chatWithAI(
                            request.getQuestion(),
                            userId,
                            context.toString()
                    ).block();
                }
            } catch (Exception e) {
                System.err.println("백엔드 GPT API 예외, FastAPI로 폴백: " + e.getMessage());
                e.printStackTrace();
                // FastAPI로 폴백
                response = fastApiService.chatWithAI(
                        request.getQuestion(),
                        userId,
                        context.toString()
                ).block();
            }

            // 대화 기록 저장 (사용자 질문)
            AIConversation userMessage = AIConversation.builder()
                    .user(user)
                    .role("user")
                    .message(request.getQuestion())
                    .build();
            conversationRepository.save(userMessage);

            // AI 응답 저장
            AIConversation aiMessage = AIConversation.builder()
                    .user(user)
                    .role("assistant")
                    .message(response)
                    .build();
            conversationRepository.save(aiMessage);

            AIChatResponse chatResponse = AIChatResponse.builder()
                    .response(response)
                    .conversationType(request.getConversationType())
                    .build();

            return ResponseEntity.ok(chatResponse);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }
}

