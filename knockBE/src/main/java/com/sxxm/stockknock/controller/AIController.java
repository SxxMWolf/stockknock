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

            // AI 응답 생성
            String response = aiService.answerQuestion(request.getQuestion(), "");

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

