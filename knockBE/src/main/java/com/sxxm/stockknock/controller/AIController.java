package com.sxxm.stockknock.controller;

import com.sxxm.stockknock.dto.AIChatRequest;
import com.sxxm.stockknock.dto.AIChatResponse;
import com.sxxm.stockknock.entity.AIConversation;
import com.sxxm.stockknock.entity.User;
import com.sxxm.stockknock.repository.AIConversationRepository;
import com.sxxm.stockknock.service.UserService;
import com.sxxm.stockknock.ai.AIService;
import com.sxxm.stockknock.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "http://localhost:3000")
public class AIController {

    @Autowired
    private AIService aiService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserService userService;

    @Autowired
    private AIConversationRepository conversationRepository;

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
                context.append("사용자: ").append(conv.getUserQuestion()).append("\n");
                context.append("AI: ").append(conv.getAiResponse()).append("\n\n");
            }

            // AI 응답 생성 (문맥 포함)
            String response = aiService.answerQuestionWithContext(
                    request.getQuestion(), 
                    context.toString(),
                    recentConversations.size()
            );

            // 대화 기록 저장
            AIConversation conversation = AIConversation.builder()
                    .user(user)
                    .userQuestion(request.getQuestion())
                    .aiResponse(response)
                    .conversationType(request.getConversationType())
                    .build();
            conversationRepository.save(conversation);

            AIChatResponse chatResponse = AIChatResponse.builder()
                    .response(response)
                    .conversationType(request.getConversationType())
                    .build();

            return ResponseEntity.ok(chatResponse);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}

