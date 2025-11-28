package com.sxxm.stockknock.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "portfolios")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Portfolio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    private BigDecimal quantity; // 보유 수량

    private BigDecimal averagePrice; // 평균 매수가

    private BigDecimal currentPrice; // 현재 가격

    private BigDecimal totalValue; // 총 평가액

    private BigDecimal profitLoss; // 손익

    private BigDecimal profitLossRate; // 손익률

    private LocalDateTime purchasedAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        purchasedAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

