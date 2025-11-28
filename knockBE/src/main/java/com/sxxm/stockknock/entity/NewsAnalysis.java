package com.sxxm.stockknock.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "news_analyses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsAnalysis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "news_id", nullable = false)
    private News news;

    @Column(columnDefinition = "TEXT")
    private String summary; // AI 요약

    @Column(columnDefinition = "TEXT")
    private String impactAnalysis; // 주가 영향 분석

    @Enumerated(EnumType.STRING)
    private Sentiment sentiment; // 긍정, 부정, 중립

    private Integer impactScore; // 영향 점수 (1-10)

    private LocalDateTime analyzedAt;

    @PrePersist
    protected void onCreate() {
        analyzedAt = LocalDateTime.now();
    }

    public enum Sentiment {
        POSITIVE, NEGATIVE, NEUTRAL
    }
}

