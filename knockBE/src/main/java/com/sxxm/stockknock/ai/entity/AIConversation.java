package com.sxxm.stockknock.ai.entity;

/**
 * AI 대화 엔티티
 * 
 * 역할:
 * - AI 주식 분석가와의 대화 기록 저장
 * - 사용자 메시지와 AI 응답 저장
 * - 대화 맥락 유지를 위한 이력 관리
 */
import com.sxxm.stockknock.auth.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_conversation")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIConversation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 10)
    private String role; // 'user', 'assistant'

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}

