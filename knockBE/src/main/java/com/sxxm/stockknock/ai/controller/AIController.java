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
    private com.sxxm.stockknock.common.service.FastApiService fastApiService;

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

            // FastAPI에서 AI 응답 생성
            String response = fastApiService.chatWithAI(
                    request.getQuestion(),
                    userId,
                    context.toString()
            ).block();

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

