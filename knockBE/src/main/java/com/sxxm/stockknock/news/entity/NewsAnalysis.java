package com.sxxm.stockknock.news.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "news_analysis")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsAnalysis {
    @Id
    @Column(name = "news_id")
    private Long newsId;

    @OneToOne
    @JoinColumn(name = "news_id", insertable = false, updatable = false)
    @MapsId
    private News news;

    @Column(columnDefinition = "TEXT")
    private String summary; // AI 요약

    @Column(name = "sentiment", length = 20)
    private String sentiment; // 'positive', 'negative', 'neutral'

    @Column(name = "impact_score")
    private Integer impactScore; // 영향 점수 (1-10)

    @Column(name = "ai_comment", columnDefinition = "TEXT")
    private String aiComment;

    @Column(name = "analyzed_at")
    private LocalDateTime analyzedAt;

    @PrePersist
    protected void onCreate() {
        if (analyzedAt == null) {
            analyzedAt = LocalDateTime.now();
        }
    }
}

