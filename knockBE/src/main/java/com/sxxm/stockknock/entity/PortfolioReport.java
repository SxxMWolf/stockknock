package com.sxxm.stockknock.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "portfolio_reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private LocalDate reportDate;

    private BigDecimal totalValue; // 총 평가액

    private BigDecimal totalProfitLoss; // 총 손익

    private BigDecimal totalProfitLossRate; // 총 손익률

    private BigDecimal riskIndex; // 리스크 지수

    @Column(columnDefinition = "TEXT")
    private String summary; // AI 요약 코멘트

    @Column(columnDefinition = "TEXT")
    private String industryDistribution; // 산업별 비중 (JSON)

    @Column(columnDefinition = "TEXT")
    private String recommendations; // 리밸런싱 제안 (JSON)

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

