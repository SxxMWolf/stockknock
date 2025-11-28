package com.sxxm.stockknock.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_recommendations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockRecommendation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Column(columnDefinition = "TEXT")
    private String reason; // 추천 이유

    private Integer score; // 추천 점수 (1-10)

    private LocalDateTime recommendedAt;

    private Boolean isViewed;

    @PrePersist
    protected void onCreate() {
        recommendedAt = LocalDateTime.now();
        isViewed = false;
    }
}

